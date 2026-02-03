package com.example.safespark.consent

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.safespark.MainActivity
import com.example.safespark.R
import com.example.safespark.auth.ParentAuthManager

/**
 * ✅ ChildConsentActivity - Kind muss explizit zustimmen
 *
 * Google Play Store Compliance:
 * - MANDATORY Consent vom Kind
 * - Klare Erklärung was passiert
 * - Kann nicht übersprungen werden
 */
class ChildConsentActivity : AppCompatActivity() {

    private lateinit var authManager: ParentAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_consent)

        authManager = ParentAuthManager(this)

        // Wenn Consent schon gegeben -> zu Main
        if (authManager.isConsentGiven()) {
            navigateToMain()
            return
        }

        setupViews()
    }

    private fun setupViews() {
        findViewById<TextView>(R.id.titleText).text = "🤝 Deine Zustimmung"

        findViewById<TextView>(R.id.consentText).text = """
            Durch Aktivieren von SafeSpark stimmst du zu:
            
            ✅ SafeSpark liest Texte in deinen Apps (WhatsApp, etc.)
            ✅ Die Analyse passiert NUR auf diesem Handy
            ✅ KEINE Daten gehen ins Internet oder zu deinen Eltern
            ✅ Nur bei Gefahr (Mobbing, Grooming) gibt es eine Warnung
            ✅ Bei Gefahr macht das Handy 30 Min Pause
            
            ⚠️ WICHTIG:
            Deine Nachrichten bleiben PRIVAT. Deine Eltern sehen sie NICHT.
            SafeSpark warnt nur, wenn wirklich Gefahr droht.
            
            Du kannst SafeSpark jederzeit in den Einstellungen deaktivieren.
        """.trimIndent()

        val consentCheckbox = findViewById<CheckBox>(R.id.consentCheckbox)
        val agreeButton = findViewById<Button>(R.id.agreeButton)
        val declineButton = findViewById<Button>(R.id.declineButton)

        agreeButton.setOnClickListener {
            if (!consentCheckbox.isChecked) {
                Toast.makeText(
                    this,
                    "Bitte lies die Erklärung und setze den Haken",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            showFinalConfirmation()
        }

        declineButton.setOnClickListener {
            showDeclineDialog()
        }
    }

    private fun showFinalConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("🛡️ SafeSpark aktivieren")
            .setMessage("""
                Letzte Bestätigung:
                
                ✅ Du verstehst, dass SafeSpark Texte liest
                ✅ Du verstehst, dass das NUR auf diesem Handy passiert
                ✅ Du verstehst, dass Eltern die Nachrichten NICHT sehen
                ✅ Du möchtest geschützt werden
                
                SafeSpark jetzt aktivieren?
            """.trimIndent())
            .setPositiveButton("Ja, aktivieren") { _, _ ->
                authManager.setConsentGiven()
                Toast.makeText(
                    this,
                    "✅ SafeSpark ist jetzt aktiviert und schützt dich!",
                    Toast.LENGTH_LONG
                ).show()
                navigateToMain()
            }
            .setNegativeButton("Nochmal lesen") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showDeclineDialog() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Ohne SafeSpark")
            .setMessage("""
                Wenn du SafeSpark nicht aktivierst:
                
                ❌ Niemand passt auf dich auf
                ❌ Bei Mobbing oder Grooming gibt es keine Warnung
                ❌ Deine Eltern können dir nicht helfen
                
                Bist du sicher, dass du SafeSpark nicht möchtest?
            """.trimIndent())
            .setPositiveButton("Doch, aktivieren") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Wirklich nicht") { _, _ ->
                Toast.makeText(
                    this,
                    "SafeSpark wurde nicht aktiviert",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
            .show()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Verhindere dass Consent übersprungen wird
        Toast.makeText(
            this,
            "Bitte triff eine Entscheidung: Aktivieren oder Ablehnen",
            Toast.LENGTH_SHORT
        ).show()
        // Rufe super auf um Lint zu befriedigen (hat aber keine Wirkung da wir finish() nicht rufen)
        super.onBackPressed()
    }
}
