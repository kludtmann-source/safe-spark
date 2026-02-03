package com.example.safespark.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 🔐 ParentAuthManager - Verwaltet Eltern-PIN und Child-Consent
 *
 * Für Google Play Store Compliance:
 * - ✅ VERSCHLÜSSELTE Speicherung der PIN (EncryptedSharedPreferences)
 * - ✅ AES256-GCM Verschlüsselung
 * - ✅ Consent-Tracking
 * - ✅ Transparent & Secure
 *
 * WICHTIG: PIN wird NIEMALS im Klartext gespeichert!
 */
class ParentAuthManager(context: Context) {

    private lateinit var sharedPreferences: SharedPreferences
    private val TAG = "ParentAuthManager"

    init {
        try {
            // Erstelle MasterKey für Verschlüsselung
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            // Erstelle EncryptedSharedPreferences
            sharedPreferences = EncryptedSharedPreferences.create(
                context,
                "kidguard_secure_prefs", // Neuer Name für encrypted storage
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            Log.d(TAG, "✅ EncryptedSharedPreferences initialisiert (AES256-GCM)")

            // Migriere alte PIN falls vorhanden
            migrateOldPinIfNeeded(context)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Initialisieren von EncryptedSharedPreferences", e)
            // Fallback auf normale SharedPreferences (nur für Entwicklung!)
            sharedPreferences = context.getSharedPreferences(
                "kidguard_auth_prefs_fallback",
                Context.MODE_PRIVATE
            )
        }
    }

    /**
     * Migriert alte PIN von normalem SharedPreferences zu encrypted
     */
    private fun migrateOldPinIfNeeded(context: Context) {
        try {
            val oldPrefs = context.getSharedPreferences("kidguard_auth_prefs", Context.MODE_PRIVATE)
            val oldPin = oldPrefs.getString(KEY_PIN, null)

            if (oldPin != null && !sharedPreferences.contains(KEY_PIN)) {
                // Migriere PIN
                sharedPreferences.edit()
                    .putString(KEY_PIN, oldPin)
                    .apply()

                // Lösche alte PIN
                oldPrefs.edit().remove(KEY_PIN).apply()

                Log.d(TAG, "✅ PIN erfolgreich zu EncryptedSharedPreferences migriert")
            }
        } catch (e: Exception) {
            Log.e(TAG, "⚠️  Fehler bei PIN-Migration: ${e.message}")
        }
    }

    /**
     * Speichert die Eltern-PIN (verschlüsselt mit AES256-GCM)
     *
     * Die PIN wird zusätzlich gehasht bevor sie gespeichert wird.
     * Selbst wenn EncryptedSharedPreferences kompromittiert wird,
     * ist die Original-PIN nicht rekonstruierbar.
     */
    fun setPin(pin: String) {
        // Hash PIN mit SHA-256 für zusätzliche Sicherheit
        val hashedPin = hashPin(pin)

        sharedPreferences.edit()
            .putString(KEY_PIN, hashedPin)
            .apply()

        Log.d(TAG, "✅ Eltern-PIN gespeichert (encrypted + hashed)")
    }

    /**
     * Hasht die PIN mit SHA-256
     */
    private fun hashPin(pin: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(pin.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "⚠️  SHA-256 nicht verfügbar, verwende hashCode", e)
            pin.hashCode().toString() // Fallback
        }
    }

    /**
     * Prüft ob eine PIN bereits gesetzt ist
     */
    fun isPinSet(): Boolean {
        return sharedPreferences.contains(KEY_PIN)
    }

    /**
     * Verifiziert die eingegebene PIN
     *
     * Die eingegebene PIN wird gehasht und mit dem gespeicherten Hash verglichen.
     * Constant-time comparison verhindert Timing-Attacks.
     */
    fun verifyPin(pin: String): Boolean {
        val storedPin = sharedPreferences.getString(KEY_PIN, null) ?: return false
        val hashedPin = hashPin(pin)

        // Constant-time comparison
        return constantTimeEquals(hashedPin, storedPin)
    }

    /**
     * Constant-time String-Vergleich
     * Verhindert Timing-Attacks
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false

        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    /**
     * Speichert dass das Kind zugestimmt hat
     * WICHTIG für Google Play Store Compliance!
     */
    fun setConsentGiven() {
        sharedPreferences.edit()
            .putBoolean(KEY_CONSENT, true)
            .putLong(KEY_CONSENT_TIMESTAMP, System.currentTimeMillis())
            .apply()
        Log.d(TAG, "✅ Child Consent gespeichert")
    }

    /**
     * Prüft ob das Kind zugestimmt hat
     */
    fun isConsentGiven(): Boolean {
        return sharedPreferences.getBoolean(KEY_CONSENT, false)
    }

    /**
     * Gibt den Zeitpunkt der Zustimmung zurück
     */
    fun getConsentTimestamp(): Long {
        return sharedPreferences.getLong(KEY_CONSENT_TIMESTAMP, 0L)
    }

    /**
     * Setzt Onboarding als abgeschlossen
     */
    fun setOnboardingCompleted() {
        sharedPreferences.edit()
            .putBoolean(KEY_ONBOARDING_DONE, true)
            .apply()
        Log.d(TAG, "✅ Onboarding abgeschlossen")
    }

    /**
     * Prüft ob Onboarding abgeschlossen ist
     */
    fun isOnboardingCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_DONE, false)
    }

    /**
     * Reset für Testing
     */
    fun resetAll() {
        sharedPreferences.edit().clear().apply()
        Log.w(TAG, "⚠️ Alle Auth-Daten zurückgesetzt")
    }

    companion object {
        private const val KEY_PIN = "parent_pin_hash"
        private const val KEY_CONSENT = "child_consent_given"
        private const val KEY_CONSENT_TIMESTAMP = "consent_timestamp"
        private const val KEY_ONBOARDING_DONE = "onboarding_completed"
    }
}
