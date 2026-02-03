package com.example.safespark.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.safespark.R
import com.example.safespark.consent.OnboardingActivity

/**
 * 🔐 ParentAuthActivity - Eltern-PIN Setup
 *
 * Google Play Store Compliance:
 * - Eltern authentifizieren sich VOR App-Nutzung
 * - Kind kann App nicht ohne Eltern-Wissen aktivieren
 */
class ParentAuthActivity : AppCompatActivity() {

    private lateinit var authManager: ParentAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_auth)

        authManager = ParentAuthManager(this)

        // Wenn PIN bereits existiert -> direkt zu Onboarding
        if (authManager.isPinSet()) {
            navigateToOnboarding()
            return
        }

        setupViews()
    }

    private fun setupViews() {
        findViewById<TextView>(R.id.titleText).text = "🔐 Eltern-PIN erstellen"
        findViewById<TextView>(R.id.descriptionText).text =
            "Erstellen Sie eine PIN, um die App zu verwalten. Nur Sie können die App-Einstellungen ändern."

        val pinInput = findViewById<EditText>(R.id.pinInput)
        val pinConfirmInput = findViewById<EditText>(R.id.pinConfirmInput)
        val setupButton = findViewById<Button>(R.id.setupButton)

        setupButton.setOnClickListener {
            val pin = pinInput.text.toString()
            val confirm = pinConfirmInput.text.toString()

            when {
                pin.isEmpty() -> {
                    Toast.makeText(this, "Bitte PIN eingeben", Toast.LENGTH_SHORT).show()
                }
                pin.length < 4 -> {
                    Toast.makeText(this, "PIN muss mindestens 4 Zeichen haben", Toast.LENGTH_SHORT).show()
                }
                pin != confirm -> {
                    Toast.makeText(this, "PINs stimmen nicht überein", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    authManager.setPin(pin)
                    Toast.makeText(this, "✅ Eltern-PIN gespeichert", Toast.LENGTH_LONG).show()
                    navigateToOnboarding()
                }
            }
        }
    }

    private fun navigateToOnboarding() {
        startActivity(Intent(this, OnboardingActivity::class.java))
        finish()
    }
}
