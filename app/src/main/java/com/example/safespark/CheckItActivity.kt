package com.example.safespark

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CheckItActivity : AppCompatActivity() {

    private val logLabel = "CheckItActivity"
    private lateinit var guardEngine: KidGuardEngine
    private val bgScope = CoroutineScope(Dispatchers.IO)
    
    private lateinit var mascotImage: ImageView
    private lateinit var statusCard: CardView
    private lateinit var primaryMessage: TextView
    private lateinit var detailsText: TextView
    private lateinit var expandSection: LinearLayout
    private lateinit var expandButton: TextView
    private lateinit var stageInfo: TextView
    private lateinit var methodInfo: TextView
    private lateinit var actionButtonOk: Button
    private lateinit var actionButtonRetry: Button
    
    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        setContentView(R.layout.activity_check_it)
        
        guardEngine = KidGuardEngine(this)
        
        wireUpViews()
        
        val receivedIntent = intent
        when (receivedIntent?.action) {
            Intent.ACTION_SEND -> handleSingleTextShare(receivedIntent)
            Intent.ACTION_SEND_MULTIPLE -> handleMultipleTextShare(receivedIntent)
            else -> handleDirectLaunch()
        }
    }
    
    private fun wireUpViews() {
        mascotImage = findViewById(R.id.imgSparkMascot)
        statusCard = findViewById(R.id.cardResultStatus)
        primaryMessage = findViewById(R.id.txtPrimaryResult)
        detailsText = findViewById(R.id.txtExplanation)
        expandSection = findViewById(R.id.layoutExpandable)
        expandButton = findViewById(R.id.btnExpandDetails)
        stageInfo = findViewById(R.id.txtStageInfo)
        methodInfo = findViewById(R.id.txtMethodInfo)
        actionButtonOk = findViewById(R.id.btnDismiss)
        actionButtonRetry = findViewById(R.id.btnCheckAnother)
        
        expandButton.setOnClickListener {
            val currentlyHidden = expandSection.visibility == View.GONE
            expandSection.visibility = if (currentlyHidden) View.VISIBLE else View.GONE
            expandButton.text = if (currentlyHidden) "▼ Weniger Details" else "▶ Mehr erfahren"
        }
        
        actionButtonOk.setOnClickListener { 
            finish() 
        }
        
        actionButtonRetry.setOnClickListener {
            val retryIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/*"
                setClass(this@CheckItActivity, CheckItActivity::class.java)
            }
            startActivity(retryIntent)
            finish()
        }
    }
    
    private fun handleSingleTextShare(receivedIntent: Intent) {
        val sharedContent = receivedIntent.getStringExtra(Intent.EXTRA_TEXT)
        
        if (sharedContent.isNullOrBlank()) {
            displayEmptyMessageError()
            return
        }
        
        Log.d(logLabel, "Processing shared text: ${sharedContent.take(50)}...")
        performAnalysis(sharedContent)
    }
    
    private fun handleMultipleTextShare(receivedIntent: Intent) {
        val textItems = receivedIntent.getStringArrayListExtra(Intent.EXTRA_TEXT)
        
        if (textItems.isNullOrEmpty()) {
            displayEmptyMessageError()
            return
        }
        
        val combined = textItems.joinToString("\n")
        Log.d(logLabel, "Processing multiple texts, combined length: ${combined.length}")
        performAnalysis(combined)
    }
    
    private fun handleDirectLaunch() {
        Log.d(logLabel, "Direct launch detected, showing demo")
        val demoText = "Bist du allein zu Hause?"
        performAnalysis(demoText)
    }
    
    private fun performAnalysis(messageText: String) {
        bgScope.launch {
            try {
                val analysisOutput = guardEngine.analyzeTextWithExplanation(
                    input = messageText,
                    appPackage = "shared_message"
                )
                
                withContext(Dispatchers.Main) {
                    renderAnalysisResult(analysisOutput)
                }
            } catch (err: Exception) {
                Log.e(logLabel, "Analysis error: ${err.message}", err)
                withContext(Dispatchers.Main) {
                    displayAnalysisError(err.message ?: "Unknown error")
                }
            }
        }
    }
    
    private fun renderAnalysisResult(result: AnalysisResult) {
        val riskLevel = determineRiskLevel(result.score)
        
        when (riskLevel) {
            RiskCategory.SAFE -> applySafeTheme()
            RiskCategory.CAUTION -> applyCautionTheme()
            RiskCategory.DANGER -> applyDangerTheme()
        }
        
        primaryMessage.text = getMessageForRisk(riskLevel)
        detailsText.text = result.explanation
        
        if (result.isRisk && result.stage.isNotEmpty()) {
            stageInfo.visibility = View.VISIBLE
            stageInfo.text = "Phase erkannt: ${translateStage(result.stage)}"
        } else {
            stageInfo.visibility = View.GONE
        }
        
        if (result.isRisk && result.detectionMethod.isNotEmpty()) {
            methodInfo.visibility = View.VISIBLE
            methodInfo.text = "Erkennungsmethode: ${result.detectionMethod}"
        } else {
            methodInfo.visibility = View.GONE
        }
        
        Log.d(logLabel, "Rendered result: score=${result.score}, risk=${result.isRisk}")
    }
    
    private fun determineRiskLevel(scoreValue: Float): RiskCategory {
        return when {
            scoreValue < 0.3f -> RiskCategory.SAFE
            scoreValue < 0.6f -> RiskCategory.CAUTION
            else -> RiskCategory.DANGER
        }
    }
    
    private fun applySafeTheme() {
        val greenBg = ContextCompat.getColor(this, android.R.color.holo_green_light)
        val greenText = ContextCompat.getColor(this, android.R.color.holo_green_dark)
        statusCard.setCardBackgroundColor(greenBg)
        primaryMessage.setTextColor(greenText)
    }
    
    private fun applyCautionTheme() {
        val yellowBg = ContextCompat.getColor(this, android.R.color.holo_orange_light)
        val yellowText = ContextCompat.getColor(this, android.R.color.holo_orange_dark)
        statusCard.setCardBackgroundColor(yellowBg)
        primaryMessage.setTextColor(yellowText)
    }
    
    private fun applyDangerTheme() {
        val redBg = ContextCompat.getColor(this, android.R.color.holo_red_light)
        val redText = ContextCompat.getColor(this, android.R.color.holo_red_dark)
        statusCard.setCardBackgroundColor(redBg)
        primaryMessage.setTextColor(redText)
    }
    
    private fun getMessageForRisk(level: RiskCategory): String {
        return when (level) {
            RiskCategory.SAFE -> "🟢 Alles gut! Spark hat nichts Auffälliges gefunden."
            RiskCategory.CAUTION -> "🟡 Hmm, da ist was. Denk kurz nach, ob das okay ist."
            RiskCategory.DANGER -> "🔴 Hey, pass auf! Das sieht nach einem Grooming-Muster aus."
        }
    }
    
    private fun translateStage(stageCode: String): String {
        return when (stageCode.uppercase()) {
            "TRUST", "STAGE_TRUST" -> "Vertrauensaufbau"
            "ISOLATION", "STAGE_ISOLATION" -> "Isolierung"
            "ASSESSMENT", "STAGE_ASSESSMENT" -> "Situationscheck"
            "DESENSITIZATION", "STAGE_DESENSITIZATION" -> "Desensibilisierung"
            "SEXUAL", "STAGE_SEXUAL" -> "Sexuelle Inhalte"
            "MAINTENANCE", "STAGE_MAINTENANCE", "SECRECY" -> "Geheimhaltung"
            "NEEDS", "STAGE_NEEDS", "GIFT" -> "Geschenke/Hilfe"
            else -> stageCode
        }
    }
    
    private fun displayEmptyMessageError() {
        primaryMessage.text = "⚠️ Keine Nachricht erhalten"
        detailsText.text = "Bitte leite eine Nachricht an Spark weiter, um sie zu überprüfen."
        statusCard.setCardBackgroundColor(
            ContextCompat.getColor(this, android.R.color.darker_gray)
        )
    }
    
    private fun displayAnalysisError(errorMsg: String) {
        primaryMessage.text = "❌ Fehler bei der Analyse"
        detailsText.text = "Es ist ein Problem aufgetreten: $errorMsg"
        statusCard.setCardBackgroundColor(
            ContextCompat.getColor(this, android.R.color.darker_gray)
        )
    }
    
    override fun onDestroy() {
        super.onDestroy()
        guardEngine.close()
    }
    
    private enum class RiskCategory {
        SAFE, CAUTION, DANGER
    }
}
