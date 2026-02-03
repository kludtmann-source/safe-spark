package com.example.safespark.database

/**
 * 🔧 Integration Guide: Room Database in GuardianAccessibilityService
 *
 * Diese Datei zeigt, wie du die Database in deinen Service integrierst.
 *
 * ============================================================================
 * SCHRITT 1: Imports hinzufügen
 * ============================================================================
 */

/*
import com.example.safespark.database.SafeSparkDatabase
import com.example.safespark.database.RiskEvent
import com.example.safespark.database.RiskEventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
*/

/**
 * ============================================================================
 * SCHRITT 2: Class-Level Variables in GuardianAccessibilityService
 * ============================================================================
 */

/*
class GuardianAccessibilityService : AccessibilityService() {

    // ... existing code ...

    // ✅ NEU: Database-Instanzen
    private lateinit var database: SafeSparkDatabase
    private lateinit var repository: RiskEventRepository

    // ... existing code ...
}
*/

/**
 * ============================================================================
 * SCHRITT 3: Database initialisieren in onCreate()
 * ============================================================================
 */

/*
override fun onCreate() {
    super.onCreate()

    // ... existing code ...

    // ✅ NEU: Initialisiere Database
    database = SafeSparkDatabase.getDatabase(this)
    repository = RiskEventRepository(database.riskEventDao())

    Log.d(TAG, "✅ Database & Repository initialisiert")

    // ... existing code ...
}
*/

/**
 * ============================================================================
 * SCHRITT 4: Event in DB speichern wenn Risiko erkannt
 * ============================================================================
 *
 * Finde die Stelle wo du aktuell sendNotification() aufrufst und füge DAVOR
 * den DB-Insert ein.
 */

/*
// In handleHighRiskDetection() oder ähnlicher Methode:

private fun handleRiskDetection(result: SafeSparkEngine.AnalysisResult, text: String) {

    // ✅ NEU: Erstelle RiskEvent
    val riskEvent = RiskEvent(
        timestamp = System.currentTimeMillis(),
        appPackage = result.appPackage,
        appName = result.appName,
        message = text,
        riskScore = result.riskScore,
        mlStage = result.mlStage,
        keywordMatches = result.keywords.joinToString(",")
    )

    // ✅ NEU: Speichere in DB (asynchron mit Coroutine)
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val eventId = repository.insert(riskEvent)
            Log.d(TAG, "✅ Risiko in DB gespeichert: ID=$eventId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Speichern in DB", e)
        }
    }

    // Existing code: Notification senden
    sendNotification(result.appName, text, result.riskScore)
}
*/

/**
 * ============================================================================
 * SCHRITT 5: Teste die Integration
 * ============================================================================
 *
 * 1. Build & Install:
 *    ./gradlew installDebug
 *
 * 2. Aktiviere AccessibilityService
 *
 * 3. Teste mit Grooming-Message:
 *    - Öffne WhatsApp/Testing-App
 *    - Schreibe: "Bist du allein?"
 *
 * 4. Prüfe Logs:
 *    adb logcat | grep "RiskEventRepository"
 *    Sollte zeigen: "✅ Event gespeichert: ID=1, ..."
 *
 * 5. Prüfe Database (Android Studio):
 *    View → Tool Windows → App Inspection → Database Inspector
 *    → kidguard_database → risk_events
 *    → Solltest 1 Eintrag sehen!
 *
 * ============================================================================
 * WICHTIG: Cleanup alter Events (DSGVO)
 * ============================================================================
 *
 * Füge in onCreate() oder onStartCommand() hinzu:
 */

/*
// Cleanup Events älter als 30 Tage
CoroutineScope(Dispatchers.IO).launch {
    repository.cleanupOldEvents(retentionDays = 30)
}
*/

/**
 * ============================================================================
 * BEISPIEL: Komplette handleHighRiskDetection() Methode
 * ============================================================================
 */

/*
private fun handleHighRiskDetection(
    result: SafeSparkEngine.AnalysisResult,
    text: String,
    packageName: String
) {
    Log.w(TAG, """
        🚨 RISIKO ERKANNT:
           App: ${result.appName} ($packageName)
           Text: "$text"
           Score: ${result.riskScore}
           Stage: ${result.mlStage}
           Keywords: ${result.keywords}
    """.trimIndent())

    // ✅ 1. In Database speichern
    val riskEvent = RiskEvent(
        timestamp = System.currentTimeMillis(),
        appPackage = packageName,
        appName = result.appName,
        message = text.take(500), // Max 500 Zeichen
        riskScore = result.riskScore,
        mlStage = result.mlStage,
        keywordMatches = result.keywords.joinToString(",")
    )

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val eventId = repository.insert(riskEvent)
            Log.d(TAG, "✅ Risiko in DB gespeichert: ID=$eventId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ DB-Fehler", e)
        }
    }

    // ✅ 2. Notification senden (existing code)
    sendNotification(result.appName, text, result.riskScore)
}
*/

/**
 * ============================================================================
 * FERTIG! 🎉
 * ============================================================================
 *
 * Nächster Schritt: Dashboard UI erstellen, um die Events anzuzeigen!
 *
 * Siehe: NAECHSTE_SCHRITTE_27_JAN.md → "MORGEN: Dashboard Layout"
 */
