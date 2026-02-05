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
    private val TAG = "GuardianAccessibility"
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val analyzedTextCache = mutableSetOf<String>()
    private val maxCacheSize = 100

    private var lastEventTime = 0L
    private val minEventInterval = 0L

    private var debugMode = true

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
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                Log.d(TAG, "  → TYPE_VIEW_FOCUSED erkannt")
                event.contentDescription?.let { texts.add(it.toString()) }
                event.text?.let { texts.addAll(it.map { t -> t.toString() }) }
            }
            else -> {
                Log.d(TAG, "  → Anderer EventType: ${event.eventType}")
                event.contentDescription?.let { texts.add(it.toString()) }
                texts.addAll(event.text.map { it.toString() })
            }
        }

        Log.d(TAG, "  📊 Extrahierte Texte: ${texts.size} Stück")

        for (text in texts) {
            if (text.isEmpty()) {
                Log.d(TAG, "  ⏭️ Leerer Text übersprungen")
                continue
            }

            // 🔥 Cache aktiviert - verhindert doppelte Analysen
            if (analyzedTextCache.contains(text)) {
                Log.d(TAG, "  ⏭️ Text bereits im Cache: '${text.take(20)}...'")
                continue
            }

            Log.d(TAG, "  🔍 ANALYSIERE TEXT: '$text'")

            // ✅ Nutze NEUE Explainable AI Methode
            val result = getEngine().analyzeTextWithExplanation(text, packageName)
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
                val stageLabel = when {
                    // Zuerst nach stage prüfen
                    result.stage.contains("TRUST", ignoreCase = true) -> "Vertrauensaufbau"
                    result.stage.contains("ISOLATION", ignoreCase = true) -> "Isolierung"
                    result.stage.contains("DESENSITIZATION", ignoreCase = true) -> "Desensibilisierung"
                    result.stage.contains("SEXUAL", ignoreCase = true) -> "Sexuelle Inhalte"
                    result.stage.contains("MAINTENANCE", ignoreCase = true) -> "Geheimhaltung"
                    result.stage.contains("ASSESSMENT", ignoreCase = true) -> "Situationscheck"
                    // Fallback: nach detectionMethod prüfen
                    result.detectionMethod.contains("Assessment", ignoreCase = true) -> "Situationscheck"
                    result.detectionMethod.contains("Semantic", ignoreCase = true) -> "Semantisch erkannt"
                    result.detectionMethod.contains("Osprey", ignoreCase = true) -> "Osprey-Erkennung"
                    result.detectionMethod.contains("ML", ignoreCase = true) -> "ML-Erkennung"
                    result.detectionMethod.contains("Trigram", ignoreCase = true) -> "Muster-Erkennung"
                    // Default
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
    }

    companion object {
        private const val TAG = "GuardianAccessibility"
    }
}
