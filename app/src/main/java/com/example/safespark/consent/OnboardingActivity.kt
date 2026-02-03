package com.example.safespark.consent

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.safespark.R
import com.example.safespark.auth.ParentAuthManager

/**
 * 📚 OnboardingActivity - Erklärt dem Kind was SafeSpark macht
 *
 * Google Play Store Compliance:
 * - Transparente Erklärung was die App tut
 * - Kind wird informiert VOR Aktivierung
 * - Keine heimliche Überwachung
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var authManager: ParentAuthManager
    private var currentPage = 0

    private val pages = listOf(
        Page(
            "🛡️ Was ist SafeSpark?",
            "SafeSpark ist dein digitaler Bodyguard. Er passt auf, dass du online sicher bist und niemand dir Böses will."
        ),
        Page(
            "👀 Was macht SafeSpark?",
            "SafeSpark liest mit, wenn du Nachrichten in Apps wie WhatsApp bekommst.\n\nABER: Deine Eltern sehen die Nachrichten NICHT!"
        ),
        Page(
            "🔒 Deine Privatsphäre",
            "Alle Nachrichten bleiben auf DIESEM Handy.\n\n✅ Nichts geht ins Internet\n✅ Niemand außer dir liest sie\n✅ Komplett privat"
        ),
        Page(
            "⚠️ Wann warnt SafeSpark?",
            "Nur wenn jemand gefährliche Dinge schreibt:\n\n• Gewalt oder Drogen\n• Mobbing oder Beleidigungen\n• Fremde die komische Sachen fragen"
        ),
        Page(
            "✋ Was passiert bei Gefahr?",
            "Das Handy macht eine kurze Pause (30 Min), damit du Zeit hast ruhig zu werden und mit deinen Eltern zu reden."
        ),
        Page(
            "🤝 Bereit?",
            "Jetzt kannst du entscheiden:\n\nMöchtest du, dass SafeSpark dich beschützt?\n\nDeine Eltern haben das schon erlaubt, aber DU entscheidest mit!"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        authManager = ParentAuthManager(this)

        // Wenn Onboarding schon fertig -> zu Consent
        if (authManager.isOnboardingCompleted()) {
            navigateToConsent()
            return
        }

        showPage(0)

        findViewById<Button>(R.id.nextButton).setOnClickListener {
            if (currentPage < pages.size - 1) {
                showPage(++currentPage)
            } else {
                authManager.setOnboardingCompleted()
                navigateToConsent()
            }
        }

        findViewById<Button>(R.id.backButton).setOnClickListener {
            if (currentPage > 0) {
                showPage(--currentPage)
            }
        }
    }

    private fun showPage(index: Int) {
        val page = pages[index]
        findViewById<TextView>(R.id.titleText).text = page.title
        findViewById<TextView>(R.id.descriptionText).text = page.description
        findViewById<TextView>(R.id.pageIndicatorText).text = "${index + 1} / ${pages.size}"

        findViewById<Button>(R.id.backButton).isEnabled = index > 0
        findViewById<Button>(R.id.nextButton).text =
            if (index == pages.size - 1) "Verstanden" else "Weiter"
    }

    private fun navigateToConsent() {
        startActivity(Intent(this, ChildConsentActivity::class.java))
        finish()
    }

    data class Page(val title: String, val description: String)
}
