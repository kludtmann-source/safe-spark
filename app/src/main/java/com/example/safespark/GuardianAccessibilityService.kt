package com.example.safespark

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.safespark.notification.NotificationHelper
// ✅ Database Integration
import com.example.safespark.database.KidGuardDatabase
import com.example.safespark.database.RiskEventRepository
import com.example.safespark.database.RiskEvent
import com.example.safespark.ml.ConversationBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.text.SimpleDateFormat
import java.util.*

class GuardianAccessibilityService : AccessibilityService() {

    private var safeSparkEngine: KidGuardEngine? = null
    private var notificationHelper: NotificationHelper? = null
    // ✅ Database Repository
    private var repository: RiskEventRepository? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val analyzedTextCache = mutableSetOf<String>()
    private val maxCacheSize = 100

    private var lastEventTime = 0L
    private val minEventInterval = 0L

    private var debugMode = true

    companion object {
        private const val TAG = "GuardianAccessibility"
        
        // Chat-Titel Extraktion: Konstanten
        private const val MAX_NODE_SEARCH_DEPTH = 10  // Begrenzt rekursive Suche aus Performance-/Sicherheitsgründen
        
        // Precompiled Regexes für bessere Performance
        private val PARENTHETICAL_REGEX = Regex("\\s*\\(.*?\\)\\s*")  // Entfernt (online), (typing)
        private val WHITESPACE_REGEX = Regex("\\s+")  // Normalisiert Whitespace
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)

        // ✅ Initialisiere Room Database Repository
        val database = KidGuardDatabase.getDatabase(this)
        repository = RiskEventRepository(database.riskEventDao())

        Log.d(TAG, "✅ Service erstellt")
        Log.d(TAG, "🔔 Notifications AKTIVIERT")
        Log.d(TAG, "💾 Database INITIALISIERT")

        // 📋 In-App-Logs (nur Debug, nicht im UI)
        // LogBuffer.i("✅ Service erstellt")
        // LogBuffer.i("💾 Database initialisiert")

        // 🔍 VERSION MARKER - nur in Logcat, nicht im UI
        Log.e(TAG, "🔥 VERSION: 2.0-ASSESSMENT-FIX-ACTIVE 🔥")
    }

    private fun getEngine(): KidGuardEngine {
        if (safeSparkEngine == null) {
            safeSparkEngine = KidGuardEngine(this)
            Log.d(TAG, "🔋 Engine initialisiert")
        }
        return safeSparkEngine!!
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.w(TAG, "🎉 onServiceConnected() - Service AKTIV!")

        val info = serviceInfo
        if (info != null) {
            info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            info.notificationTimeout = 0
            serviceInfo = info

            Log.w(TAG, "📡 EventTypes set, Flags set")
        }

        Log.w(TAG, "📡 Service empfängt Events + Notifications!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) {
            Log.w(TAG, "⚠️ onAccessibilityEvent aufgerufen mit NULL-Event!")
            return
        }

        // 🔍 ULTRA-DEBUG: Rohdaten des Events IMMER loggen
        try {
            val eventTypeName = AccessibilityEvent.eventTypeToString(event.eventType)
            val rawText = event.text?.joinToString(separator = " | ") { it.toString() } ?: ""
            val contentDesc = event.contentDescription?.toString() ?: ""
            val beforeText = event.beforeText?.toString() ?: ""

            Log.w(TAG, "━━━ [RAW EVENT START] ━━━")
            Log.w(TAG, "  📱 Package: ${event.packageName}")
            Log.w(TAG, "  📝 EventType: $eventTypeName (${event.eventType})")
            Log.w(TAG, "  📄 Text: [$rawText]")
            Log.w(TAG, "  📄 ContentDesc: [$contentDesc]")
            Log.w(TAG, "  📄 BeforeText: [$beforeText]")
            Log.w(TAG, "━━━ [RAW EVENT END] ━━━")

            // 📋 In-App-Log (nur wenn Text vorhanden)
            if (rawText.isNotEmpty() || contentDesc.isNotEmpty()) {
                val displayText = if (rawText.isNotEmpty()) rawText else contentDesc
                LogBuffer.d("📱 ${event.packageName}: '${displayText.take(40)}'")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Lesen des AccessibilityEvent: ${e.message}", e)
            LogBuffer.e("❌ Event-Fehler: ${e.message}")
        }

        val currentTime = System.currentTimeMillis()
        if (minEventInterval > 0 && currentTime - lastEventTime < minEventInterval) {
            Log.d(TAG, "⏭️ Event übersprungen (Interval-Filter)")
            return
        }
        lastEventTime = currentTime

        val timestamp = dateFormat.format(Date())
        val packageName = event.packageName?.toString() ?: "unknown"

        // 🚫 SYSTEM-PACKAGES IGNORIEREN (keine Grooming-Detection für System-Dialoge!)
        val ignoredPackages = listOf(
            "android",
            "com.android.systemui",
            "com.google.android.inputmethod",
            "com.samsung.android.inputmethod",
            "com.example.safespark"  // Eigene App auch ignorieren
        )
        if (ignoredPackages.any { packageName.startsWith(it) }) {
            Log.d(TAG, "⏭️ System-Package ignoriert: $packageName")
            return
        }

        val texts = mutableListOf<String>()
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                Log.d(TAG, "  → TYPE_VIEW_TEXT_CHANGED erkannt")
                texts.addAll(event.text.map { it.toString() })
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                Log.d(TAG, "  → TYPE_WINDOW_CONTENT_CHANGED erkannt")
                event.contentDescription?.let { texts.add(it.toString()) }
                texts.addAll(event.text.map { it.toString() })

                // 🔥 NEUE LOGIK: Extrahiere auch aus Source-View (für WhatsApp/Telegram)
                event.source?.let { node ->
                    extractTextFromNode(node, texts)
                }
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                Log.d(TAG, "  → TYPE_VIEW_FOCUSED erkannt")
                event.contentDescription?.let { texts.add(it.toString()) }
                event.text?.let { texts.addAll(it.map { t -> t.toString() }) }
            }
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                Log.d(TAG, "  → TYPE_NOTIFICATION_STATE_CHANGED erkannt")
                event.text?.let { texts.addAll(it.map { t -> t.toString() }) }
            }
            else -> {
                Log.d(TAG, "  → Anderer EventType: ${event.eventType}")
                event.contentDescription?.let { texts.add(it.toString()) }
                texts.addAll(event.text.map { it.toString() })
            }
        }

        Log.d(TAG, "  📊 Extrahierte Texte: ${texts.size} Stück")

        // 🔥 NEUE LOGIK: Chat-Titel extrahieren für per-Contact-Buffers
        val chatTitle = extractChatTitle(event, packageName)
        Log.d(TAG, "  💬 Chat-Identifier: '$chatTitle'")

        // Heuristik: TYPE_VIEW_TEXT_CHANGED = Kind tippt selbst → isLocalUser = true
        //            TYPE_WINDOW_CONTENT_CHANGED = empfangene Nachricht → isLocalUser = false
        //            Andere Events → false (konservativer Fallback)
        val isLocalUser = when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> true
            else -> false
        }

        for (text in texts) {
            if (text.isEmpty()) {
                Log.d(TAG, "  ⏭️ Leerer Text übersprungen")
                continue
            }
            
            // 🔥 NEUE LOGIK: UI-Text-Filter - ignoriere Android System UI Strings
            if (isSystemUIText(text)) {
                Log.d(TAG, "  ⏭️ System-UI-Text übersprungen: '${text.take(30)}...'")
                continue
            }

            // 🔥 Cache aktiviert - verhindert doppelte Analysen
            if (analyzedTextCache.contains(text)) {
                Log.d(TAG, "  ⏭️ Text bereits im Cache: '${text.take(20)}...'")
                continue
            }

            Log.d(TAG, "  🔍 ANALYSIERE TEXT: '$text'")

            // ✅ Nutze Konversations-basierte Analyse für Osprey-Integration
            // chatIdentifier: Nutze extrahierten Chat-Titel für per-Contact-Buffers
            val result = getEngine().analyzeWithConversation(
                input = text,
                appPackage = packageName,
                chatIdentifier = chatTitle,  // ✅ Chat-Titel aus UI extrahiert (Fallback: packageName)
                isLocalUser = isLocalUser  // ← Heuristik statt hardcoded false
            )
            val scorePercent = (result.score * 100).toInt()

            // 🔥 NUR bei positivem Finding loggen (nicht jede Analyse!)
            if (!result.isRisk) {
                Log.d(TAG, "[$timestamp] ✅ Safe: '$text' (Score: ${result.score}, Source: $packageName)")
                // KEIN LogBuffer für Safe-Analysen - nur Findings anzeigen!
            }

            analyzedTextCache.add(text)
            if (analyzedTextCache.size > maxCacheSize) {
                analyzedTextCache.remove(analyzedTextCache.first())
            }

            if (result.isRisk) {
                // 📋 EIN konsolidierter Log-Eintrag pro Finding
                // Stage-Label basierend auf stage ODER detectionMethod
                val stageLower = result.stage.lowercase()
                val methodLower = result.detectionMethod.lowercase()

                val stageLabel = when {
                    // Trust Building (verschiedene Varianten)
                    stageLower.contains("trust") -> "Vertrauensaufbau"
                    stageLower.contains("compliment") -> "Vertrauensaufbau"
                    methodLower.contains("trust") -> "Vertrauensaufbau"

                    // Isolation
                    stageLower.contains("isolation") -> "Isolierung"
                    stageLower.contains("supervision") -> "Isolierung"
                    methodLower.contains("isolation") -> "Isolierung"

                    // Desensitization
                    stageLower.contains("desensitization") -> "Desensibilisierung"
                    stageLower.contains("desensit") -> "Desensibilisierung"

                    // Sexual Content
                    stageLower.contains("sexual") -> "Sexuelle Inhalte"
                    stageLower.contains("photo") -> "Sexuelle Inhalte"

                    // Maintenance/Secrecy
                    stageLower.contains("maintenance") -> "Geheimhaltung"
                    stageLower.contains("secrecy") -> "Geheimhaltung"
                    stageLower.contains("secret") -> "Geheimhaltung"

                    // Assessment
                    stageLower.contains("assessment") -> "Situationscheck"
                    methodLower.contains("assessment") -> "Situationscheck"

                    // Needs/Gift Giving
                    stageLower.contains("needs") -> "Geschenke/Hilfe"
                    stageLower.contains("gift") -> "Geschenke/Hilfe"

                    // Fallback: nach detectionMethod prüfen
                    methodLower.contains("semantic") -> "Semantisch erkannt"
                    methodLower.contains("osprey") -> "Osprey-Erkennung"
                    methodLower.contains("machine") || methodLower.contains("ml") -> "ML-Erkennung"
                    methodLower.contains("trigram") -> "Muster-Erkennung"
                    methodLower.contains("multi") -> "Multi-Layer"

                    // Default mit Stage-Info falls vorhanden
                    result.stage.isNotEmpty() && result.stage != "UNKNOWN" -> result.stage
                    else -> "Erkannt"
                }

                LogBuffer.e("🚨 $stageLabel | ${scorePercent}% | '${text.take(40)}'")

                // ✅ Speichere in Datenbank
                saveRiskEventToDatabase(packageName, text, result.score)

                // Sende Notification (ohne doppeltes Logging)
                sendRiskNotification(packageName, result.score, timestamp, result.explanation)
            }
        }
    }

    private fun sendRiskNotification(packageName: String, score: Float, timestamp: String, explanation: String = "") {
        val scorePercent = (score * 100).toInt()

        // KEIN LogBuffer hier - wird schon oben geloggt!

        try {
            val appName = when {
                packageName.contains("whatsapp") -> "WhatsApp"
                packageName.contains("telegram") -> "Telegram"
                packageName.contains("signal") -> "Signal"
                packageName.contains("messenger") -> "Messenger"
                packageName.contains("instagram") -> "Instagram"
                packageName.contains("tiktok") -> "TikTok"
                packageName.contains("snapchat") -> "Snapchat"
                else -> packageName.substringAfterLast(".").replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            }

            if (notificationHelper != null) {
                notificationHelper?.sendRiskNotification(appName, score, timestamp)
                Log.w(TAG, "🔔 Notification gesendet für: $appName (Score: ${scorePercent}%)")
                // KEIN LogBuffer - Finding wurde schon geloggt
            } else {
                Log.e(TAG, "❌ NotificationHelper ist NULL!")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Senden der Notification: ${e.message}", e)
        }
    }

    /**
     * Speichert RiskEvent in Room Database
     * Wird asynchron ausgeführt (Thread)
     * ✅ AKTIVIERT - Room Database Integration
     */
    private fun saveRiskEventToDatabase(packageName: String, messageText: String, riskScore: Float) {
        repository?.let { repo ->
            Thread {
                try {
                    val appName = getAppName(packageName)

                    // ML-Stage basierend auf Score (Vereinfacht für MVP)
                    val mlStage = when {
                        riskScore >= 0.85f -> "STAGE_ASSESSMENT"
                        riskScore >= 0.75f -> "STAGE_ISOLATION"
                        riskScore >= 0.65f -> "STAGE_NEEDS"
                        riskScore >= 0.55f -> "STAGE_TRUST"
                        else -> "STAGE_SAFE"
                    }

                    // Erstelle RiskEvent
                    val riskEvent = RiskEvent(
                        timestamp = System.currentTimeMillis(),
                        appPackage = packageName,
                        appName = appName,
                        message = messageText.take(500),
                        riskScore = riskScore,
                        mlStage = mlStage,
                        keywordMatches = ""
                    )

                    val eventId = repo.insert(riskEvent)

                    Log.d(TAG, "💾 RiskEvent gespeichert in DB (ID: $eventId)")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Fehler beim Speichern in DB: ${e.message}", e)
                }
            }.start()
        } ?: Log.w(TAG, "⚠️ Repository nicht initialisiert")
    }

    /**
     * Mappt Package-Name zu lesbarem App-Namen
     */
    private fun getAppName(packageName: String): String {
        return when {
            packageName.contains("whatsapp") -> "WhatsApp"
            packageName.contains("telegram") -> "Telegram"
            packageName.contains("signal") -> "Signal"
            packageName.contains("messenger") -> "Messenger"
            packageName.contains("instagram") -> "Instagram"
            packageName.contains("tiktok") -> "TikTok"
            packageName.contains("snapchat") -> "Snapchat"
            packageName.contains("discord") -> "Discord"
            else -> packageName.substringAfterLast(".").replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
        // DSGVO: Buffer leeren bei Interrupt
        ConversationBuffer.clearAll()
    }

    /**
     * 🔥 Extrahiert Chat-Titel aus AccessibilityEvent
     * 
     * Versucht den Chat-Titel aus verschiedenen Quellen zu extrahieren:
     * 1. Window-Titel (für WhatsApp, Telegram, etc.)
     * 2. View-Hierarchie (Toolbar/ActionBar)
     * 3. Fallback: packageName
     * 
     * DSGVO: Der Titel wird im Klartext zurückgegeben, aber vom Aufrufer
     * sofort an generateContactId() übergeben, wo er gehasht wird.
     * Der Klartext-Titel wird nie persistent gespeichert.
     * 
     * @param event AccessibilityEvent
     * @param packageName Fallback wenn kein Titel gefunden
     * @return Chat-Titel oder packageName als Fallback
     */
    private fun extractChatTitle(event: AccessibilityEvent, packageName: String): String {
        try {
            // 1. Versuche Window-Titel (funktioniert für viele Messenger)
            val source = event.source
            if (source != null) {
                // Window-Titel abrufen
                val window = source.window
                if (window != null) {
                    val windowTitle = window.title?.toString()
                    if (!windowTitle.isNullOrBlank() && isValidChatTitle(windowTitle)) {
                        Log.d(TAG, "📱 Chat-Titel aus Window: '$windowTitle'")
                        return sanitizeChatTitle(windowTitle)
                    }
                }
                
                // 2. Durchsuche View-Hierarchie nach Toolbar/ActionBar
                val chatTitle = findChatTitleInNodeTree(source)
                if (chatTitle != null) {
                    Log.d(TAG, "📱 Chat-Titel aus Node-Tree: '$chatTitle'")
                    return chatTitle
                }
            }
            
            Log.d(TAG, "⚠️ Kein Chat-Titel gefunden, nutze Package als Fallback")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Fehler bei Chat-Titel-Extraktion: ${e.message}")
        }
        
        // Fallback: packageName
        return packageName
    }
    
    /**
     * Durchsucht Node-Tree nach Chat-Titel
     * Sucht nach typischen ViewId-Patterns für Messenger-Toolbars
     */
    private fun findChatTitleInNodeTree(node: android.view.accessibility.AccessibilityNodeInfo?): String? {
        if (node == null) return null
        
        try {
            // Typische ViewIds für Chat-Titel in Messengern
            val titleViewIds = listOf(
                "action_bar_title",
                "conversation_title",
                "contact_name",
                "chat_title",
                "toolbar_title",
                "title",
                "header_title"
            )
            
            // Prüfe ob dieser Node einen Chat-Titel enthält
            val viewId = node.viewIdResourceName?.toString() ?: ""
            if (titleViewIds.any { viewId.contains(it, ignoreCase = true) }) {
                val text = node.text?.toString()
                if (!text.isNullOrBlank() && isValidChatTitle(text)) {
                    return sanitizeChatTitle(text)
                }
                
                val contentDesc = node.contentDescription?.toString()
                if (!contentDesc.isNullOrBlank() && isValidChatTitle(contentDesc)) {
                    return sanitizeChatTitle(contentDesc)
                }
            }
            
            // Rekursiv durch Kinder (begrenzte Tiefe)
            for (i in 0 until minOf(node.childCount, MAX_NODE_SEARCH_DEPTH)) {
                node.getChild(i)?.let { child ->
                    val result = findChatTitleInNodeTree(child)
                    child.recycle()
                    if (result != null) return result
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fehler bei Node-Tree-Suche: ${e.message}")
        }
        
        return null
    }
    
    /**
     * Prüft ob ein String ein valider Chat-Titel ist
     * Filtert System-Strings und ungültige Werte aus
     */
    private fun isValidChatTitle(title: String): Boolean {
        val trimmed = title.trim()
        
        // Zu kurz oder zu lang
        if (trimmed.length < 2 || trimmed.length > 100) return false
        
        // System-Strings filtern
        val invalidPatterns = listOf(
            "whatsapp", "telegram", "signal", "messenger", "instagram",
            "loading", "connecting", "null", "undefined",
            "android", "system"
        )
        
        val lowerTitle = trimmed.lowercase()
        if (invalidPatterns.any { lowerTitle == it }) return false
        
        return true
    }
    
    /**
     * Bereinigt Chat-Titel von Sonderzeichen und Status-Infos
     * z.B. "Max (online)" -> "Max"
     */
    private fun sanitizeChatTitle(title: String): String {
        return title.trim()
            .replace(PARENTHETICAL_REGEX, "") // Entferne (online), (typing), etc.
            .replace(WHITESPACE_REGEX, " ") // Normalisiere Whitespace
            .trim()
    }

    /**
     * 🔥 Rekursive Text-Extraktion aus AccessibilityNodeInfo
     * Durchsucht die View-Hierarchie nach Text (z.B. für WhatsApp/Telegram)
     */
    private fun extractTextFromNode(node: android.view.accessibility.AccessibilityNodeInfo?, texts: MutableList<String>, depth: Int = 0) {
        if (node == null || depth > 5) return  // Max. Tiefe 5 gegen Endlosschleifen

        try {
            // Text aus diesem Node extrahieren
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
            node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }

            // Rekursiv durch Kinder gehen
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    extractTextFromNode(child, texts, depth + 1)
                    child.recycle()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fehler bei Text-Extraktion aus Node: ${e.message}")
        }
    }
    
    /**
     * 🔥 UI-Text-Filter
     * Prüft ob ein Text zu Android System UI gehört und nicht analysiert werden sollte
     * 
     * Filtert typische UI-Strings wie:
     * - "Weitere Optionen", "More options"
     * - "Einstellungen", "Settings"
     * - Menü-Texte, Buttons, Navigationsleisten
     * - Accessibility-Labels
     * 
     * @param text Der zu prüfende Text
     * @return true wenn System-UI-Text, false wenn Chat-Nachricht
     */
    private fun isSystemUIText(text: String): Boolean {
        val lowerText = text.lowercase().trim()
        
        // Zu kurz für echte Nachrichten (aber zu lang für einzelne Wörter wie "ok")
        if (lowerText.length < 3) return true
        
        // System-UI-Patterns (Deutsch & Englisch)
        val systemUIPatterns = listOf(
            // Menü & Optionen
            "weitere optionen", "more options", "optionen für",
            "einstellungen", "settings",
            "menü", "menu",
            // Navigation
            "navigationsleiste", "navigation",
            "zurück", "back",
            "schließen", "close", "öffnen", "open",
            // UI-Elemente
            "schaltfläche", "button",
            "benachrichtigung", "notification",
            "suchen", "search",
            "teilen", "share",
            "kopieren", "copy",
            "einfügen", "paste",
            // Status-Texte
            "wird geladen", "loading",
            "verbinden", "connecting",
            "online", "offline", "typing",
            // Accessibility
            "bild", "image", "icon"
        )
        
        // Prüfe exakte Matches und Teilstrings
        for (pattern in systemUIPatterns) {
            if (lowerText == pattern || lowerText.contains(pattern)) {
                return true
            }
        }
        
        // Pattern: "Optionen für 'XYZ'" - typischer Accessibility-String
        if (lowerText.matches(Regex(".*optionen für.*")) || 
            lowerText.matches(Regex(".*options for.*"))) {
            return true
        }
        
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        // DSGVO: Buffer leeren bei Service-Stop
        ConversationBuffer.clearAll()
        safeSparkEngine?.close()
        safeSparkEngine = null
    }
}
