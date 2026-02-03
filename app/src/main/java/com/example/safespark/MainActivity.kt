package com.example.safespark

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.ScrollView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safespark.auth.ParentAuthActivity
import com.example.safespark.auth.ParentAuthManager

class MainActivity : AppCompatActivity() {

    private lateinit var safeSparkEngine: KidGuardEngine
    private lateinit var authManager: ParentAuthManager
    private val TAG = "MainActivity"

    // 📋 Live-Log UI Components
    private lateinit var textLogs: TextView
    private lateinit var scrollLogs: ScrollView
    private lateinit var btnClearLogs: Button
    private val logUpdateHandler = Handler(Looper.getMainLooper())
    private val logUpdateInterval = 500L // 500ms refresh

    // 🔔 Notification Permission Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "✅ Notification Permission gewährt")
        } else {
            Log.w(TAG, "⚠️ Notification Permission verweigert")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔐 GOOGLE PLAY STORE COMPLIANCE: Prüfe Auth & Consent ZUERST
        authManager = ParentAuthManager(this)

        if (!checkAuthAndConsent()) {
            return // Flow wurde gestartet, Activity beendet sich
        }

        // Normal App Flow
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialisiere KidGuardEngine
        safeSparkEngine = KidGuardEngine(this)
        Log.d(TAG, "✅ MainActivity: KidGuardEngine initialisiert")

        // 🔔 Fordere Notification Permission an (Android 13+)
        requestNotificationPermission()

        // TEMPORÄR: Einfache UI statt Dashboard (wegen KSP-Problem)
        // if (savedInstanceState == null) {
        //     supportFragmentManager.beginTransaction()
        //         .replace(R.id.fragment_container, com.example.safespark.ui.DashboardFragment())
        //         .commit()
        // }

        setupPrivacyDashboardButton()

        // 📋 Setup Live-Log Viewer
        setupLiveLogViewer()
    }

    /**
     * 📋 Setup Live-Log Viewer (Auto-Refresh alle 500ms)
     */
    private fun setupLiveLogViewer() {
        textLogs = findViewById<TextView>(R.id.textLogs)
        scrollLogs = findViewById<ScrollView>(R.id.scrollLogs)
        btnClearLogs = findViewById<Button>(R.id.btnClearLogs)

        // Clear-Button Handler
        btnClearLogs.setOnClickListener {
            LogBuffer.clear()
            addInitialLogs()
            Log.d(TAG, "📋 Logs gelöscht")
        }

        // Füge Initial-Logs hinzu (damit sofort sichtbar)
        addInitialLogs()

        // Starte Auto-Refresh
        startLogUpdateLoop()

        // Starte Service-Status-Check (alle 2 Sekunden)
        startServiceStatusCheck()

        Log.d(TAG, "📋 Live-Log Viewer aktiviert")
    }

    /**
     * 📋 Fügt Initial-Logs hinzu
     */
    private fun addInitialLogs() {
        LogBuffer.i("━━━━━━━━━━━━━━━━━━━━━━━━━")
        LogBuffer.i("🛡️  SafeSpark gestartet")
        LogBuffer.i("━━━━━━━━━━━━━━━━━━━━━━━━━")
        LogBuffer.w("📋 Live-Log Viewer AKTIV")
        LogBuffer.i("🔄 Auto-Refresh: ${logUpdateInterval}ms")
        LogBuffer.i("")
        LogBuffer.w("⚠️  WICHTIG:")
        LogBuffer.w("Aktiviere Accessibility Service!")
        LogBuffer.w("Settings → Accessibility → SafeSpark")
        LogBuffer.i("")
        LogBuffer.d("Prüfe Service-Status...")
    }

    /**
     * 🔍 Prüft alle 2 Sekunden ob Service verbunden ist
     */
    private fun startServiceStatusCheck() {
        logUpdateHandler.postDelayed(object : Runnable {
            override fun run() {
                checkAccessibilityServiceStatus()
                logUpdateHandler.postDelayed(this, 2000L)
            }
        }, 2000L)
    }

    private var lastServiceStatus = false

    /**
     * 🔍 Prüft ob AccessibilityService läuft
     */
    private fun checkAccessibilityServiceStatus() {
        try {
            val enabledServices = android.provider.Settings.Secure.getString(
                contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )

            val isEnabled = enabledServices?.contains("com.example.safespark/.GuardianAccessibilityService") == true

            if (isEnabled && !lastServiceStatus) {
                LogBuffer.i("━━━━━━━━━━━━━━━━━━━━━━━━━")
                LogBuffer.i("✅ Service AKTIV!")
                LogBuffer.i("━━━━━━━━━━━━━━━━━━━━━━━━━")
                LogBuffer.i("Warte auf Events...")
            } else if (!isEnabled && lastServiceStatus) {
                LogBuffer.e("━━━━━━━━━━━━━━━━━━━━━━━━━")
                LogBuffer.e("❌ Service NICHT aktiv!")
                LogBuffer.e("━━━━━━━━━━━━━━━━━━━━━━━━━")
                LogBuffer.w("Aktiviere in Settings!")
            }

            lastServiceStatus = isEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Fehler: ${e.message}")
        }
    }

    /**
     * 🔄 Auto-Refresh Loop für Live-Logs
     */
    private fun startLogUpdateLoop() {
        logUpdateHandler.post(object : Runnable {
            override fun run() {
                updateLogDisplay()
                logUpdateHandler.postDelayed(this, logUpdateInterval)
            }
        })
    }

    /**
     * 🔄 Aktualisiert Log-Display
     */
    private fun updateLogDisplay() {
        val formattedLogs = LogBuffer.getFormattedText()

        if (formattedLogs.isNotEmpty() && textLogs.text.toString() != formattedLogs) {
            textLogs.text = formattedLogs
            scrollLogs.post {
                scrollLogs.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        logUpdateHandler.removeCallbacksAndMessages(null)
    }

    /**
     * 🔐 GOOGLE PLAY STORE COMPLIANCE
     * Prüft ob Eltern-PIN und Kind-Consent vorhanden sind
     *
     * @return true wenn alles OK, false wenn Flow gestartet wurde
     */
    private fun checkAuthAndConsent(): Boolean {
        when {
            !authManager.isPinSet() -> {
                Log.d(TAG, "⚠️ Keine PIN gesetzt -> ParentAuthActivity")
                startActivity(Intent(this, ParentAuthActivity::class.java))
                finish()
                return false
            }
            !authManager.isOnboardingCompleted() || !authManager.isConsentGiven() -> {
                Log.d(TAG, "⚠️ Onboarding/Consent fehlt -> Navigiere zu Flow")
                startActivity(Intent(this, com.example.safespark.consent.OnboardingActivity::class.java))
                finish()
                return false
            }
            else -> {
                Log.d(TAG, "✅ Auth & Consent OK - App kann starten")
                return true
            }
        }
    }

    /**
     * 🔒 Privacy Dashboard Button - Zeigt "0 Daten gesendet"
     */
    private fun setupPrivacyDashboardButton() {
        // Optional: Füge Menu-Item hinzu für Privacy Dashboard
        // Kann später über Options-Menu aufgerufen werden
    }

    /**
     * 🔔 Fordert Notification Permission an (Android 13+)
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "✅ Notification Permission bereits gewährt")
                }
                else -> {
                    // Fordere Permission an
                    Log.d(TAG, "🔔 Fordere Notification Permission an...")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d(TAG, "ℹ️ Android < 13 - Keine Notification Permission nötig")
        }
    }
}

/**
 * 📋 Thread-safe Log-Buffer für In-App-Anzeige
 * Singleton - speichert die letzten 200 Log-Zeilen
 */
object LogBuffer {
    private const val MAX_LOGS = 200
    private val logs = mutableListOf<LogEntry>()
    private val dateFormat = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US)

    data class LogEntry(
        val timestamp: String,
        val level: LogLevel,
        val message: String
    )

    enum class LogLevel {
        DEBUG, INFO, WARNING, ERROR
    }

    @Synchronized
    fun add(level: LogLevel, message: String) {
        val timestamp = dateFormat.format(java.util.Date())
        val entry = LogEntry(timestamp, level, message)

        logs.add(entry)

        // Behalte nur letzte MAX_LOGS
        while (logs.size > MAX_LOGS) {
            logs.removeAt(0)
        }
    }

    fun d(message: String) = add(LogLevel.DEBUG, message)
    fun i(message: String) = add(LogLevel.INFO, message)
    fun w(message: String) = add(LogLevel.WARNING, message)
    fun e(message: String) = add(LogLevel.ERROR, message)

    @Synchronized
    fun getAll(): List<LogEntry> = logs.toList()

    @Synchronized
    fun clear() = logs.clear()

    /**
     * Formatiert Logs für TextView (mit Emoji-Icons)
     */
    fun getFormattedText(): String {
        return getAll().joinToString("\n") { entry ->
            val icon = when (entry.level) {
                LogLevel.DEBUG -> "🔹"
                LogLevel.INFO -> "ℹ️"
                LogLevel.WARNING -> "⚠️"
                LogLevel.ERROR -> "🔴"
            }
            "${entry.timestamp} $icon ${entry.message}"
        }
    }
}

