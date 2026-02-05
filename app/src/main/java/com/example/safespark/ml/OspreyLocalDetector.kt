package com.example.safespark.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Osprey Local Detector - On-Device Transformer-basierte Grooming-Erkennung
 *
 * Osprey (GitHub: fani-lab/Osprey) nutzt BERT/RoBERTa für 6-Stage-Erkennung:
 * 1. SAFE - Keine Gefahr
 * 2. TRUST_BUILDING - Vertrauensaufbau (Compliments, Empathy)
 * 3. ISOLATION - Isolierung vom Umfeld
 * 4. DESENSITIZATION - Desensibilisierung (sexuelle Themen normalisieren)
 * 5. SEXUAL_CONTENT - Explizite sexuelle Inhalte
 * 6. MAINTENANCE - Geheimhaltung erzwingen
 *
 * @param context Android Context für Asset-Zugriff
 */
class OspreyLocalDetector(private val context: Context) : Closeable {

    companion object {
        private const val TAG = "OspreyDetector"
        private const val MODEL_FILE = "osprey_grooming.tflite"
        private const val MAX_SEQUENCE_LENGTH = 128

        // Osprey Stage-Definitionen (basierend auf fani-lab/Osprey)
        val STAGES = listOf(
            "SAFE",
            "TRUST_BUILDING",
            "ISOLATION",
            "DESENSITIZATION",
            "SEXUAL_CONTENT",
            "MAINTENANCE"
        )

        // Risk-Thresholds pro Stage
        private val STAGE_THRESHOLDS = mapOf(
            "SAFE" to 0.0f,
            "TRUST_BUILDING" to 0.5f,
            "ISOLATION" to 0.7f,
            "DESENSITIZATION" to 0.8f,
            "SEXUAL_CONTENT" to 0.9f,
            "MAINTENANCE" to 0.85f
        )
    }

    private var interpreter: Interpreter? = null
    private val simpleTokenizer = SimpleTokenizer()

    /**
     * Ergebnis der Osprey-Analyse
     */
    data class OspreyResult(
        val stage: String,
        val confidence: Float,
        val isRisk: Boolean,
        val allStageScores: Map<String, Float>,
        val explanation: String,
        val contextFeatures: Map<String, Float> = emptyMap(),
        val conversationLength: Int = 1,
        val dominantProgression: String? = null
    )

    init {
        try {
            loadModel()
            Log.d(TAG, "✅ Osprey Local Detector initialisiert")
            Log.d(TAG, "   Stages: ${STAGES.joinToString(", ")}")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Osprey-Modell nicht verfügbar (${e.message})")
            Log.w(TAG, "   Hinweis: Konvertiere Osprey-Modell zu TFLite und lege es in assets/ ab")
            // Kein Fehler werfen - graceful degradation
        }
    }

    /**
     * Lädt das TFLite-Modell aus assets/
     */
    private fun loadModel() {
        val modelBuffer = context.assets.openFd(MODEL_FILE).use { fileDescriptor ->
            fileDescriptor.createInputStream().use { inputStream ->
                val buffer = ByteBuffer.allocateDirect(fileDescriptor.length.toInt())
                buffer.order(ByteOrder.nativeOrder())
                inputStream.channel.read(buffer)
                buffer
            }
        }

        interpreter = Interpreter(modelBuffer, Interpreter.Options().apply {
            setNumThreads(4)
            setUseNNAPI(true)  // Nutze Android Neural Networks API falls verfügbar
        })

        Log.d(TAG, "✅ Osprey TFLite-Modell geladen")
    }

    /**
     * Analysiert Text mit Osprey-Modell
     *
     * @param text Zu analysierender Text
     * @return OspreyResult mit Stage und Confidence
     */
    fun predict(text: String): OspreyResult? {
        if (interpreter == null) {
            Log.w(TAG, "⚠️ Osprey nicht verfügbar - Modell nicht geladen")
            return null
        }

        if (text.isBlank()) {
            return OspreyResult(
                stage = "SAFE",
                confidence = 1.0f,
                isRisk = false,
                allStageScores = mapOf("SAFE" to 1.0f),
                explanation = "Leerer Text"
            )
        }

        try {
            // 1. Tokenize Text (simplified BERT tokenization)
            val tokens = simpleTokenizer.tokenize(text, MAX_SEQUENCE_LENGTH)

            // 2. Create input tensor [batch_size=1, seq_length=128]
            val inputBuffer = ByteBuffer.allocateDirect(4 * MAX_SEQUENCE_LENGTH)
            inputBuffer.order(ByteOrder.nativeOrder())

            tokens.forEach { token ->
                inputBuffer.putInt(token)
            }

            // Padding mit 0
            repeat(MAX_SEQUENCE_LENGTH - tokens.size) {
                inputBuffer.putInt(0)
            }

            inputBuffer.rewind()

            // 3. Run inference
            val outputArray = Array(1) { FloatArray(STAGES.size) }
            interpreter?.run(inputBuffer, outputArray)

            val scores = outputArray[0]

            // 4. Find max stage
            val maxIdx = scores.indices.maxByOrNull { scores[it] } ?: 0
            val stage = STAGES[maxIdx]
            val confidence = scores[maxIdx]

            // 5. Create stage map
            val allStageScores = STAGES.mapIndexed { idx, stageName ->
                stageName to scores[idx]
            }.toMap()

            // 6. Determine risk
            val threshold = STAGE_THRESHOLDS[stage] ?: 0.5f
            val isRisk = confidence > threshold && stage != "SAFE"

            // 7. Generate explanation
            val explanation = generateExplanation(stage, confidence)

            Log.d(TAG, "🔍 Osprey: $stage (${(confidence * 100).toInt()}%)")
            if (isRisk) {
                Log.w(TAG, "   ⚠️ RISK: $explanation")
            }

            return OspreyResult(
                stage = stage,
                confidence = confidence,
                isRisk = isRisk,
                allStageScores = allStageScores,
                explanation = explanation
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Prediction fehlgeschlagen", e)
            return null
        }
    }

    /**
     * Generiert menschenlesbare Erklärung
     */
    private fun generateExplanation(stage: String, confidence: Float): String {
        val confidencePercent = (confidence * 100).toInt()

        return when (stage) {
            "SAFE" -> "Keine verdächtigen Muster erkannt"
            "TRUST_BUILDING" -> "Vertrauensaufbau-Phase: Täter versucht Beziehung aufzubauen ($confidencePercent% Konfidenz)"
            "ISOLATION" -> "Isolierungs-Phase: Versuch, Opfer von Unterstützungsnetzwerk zu trennen ($confidencePercent% Konfidenz)"
            "DESENSITIZATION" -> "Desensibilisierungs-Phase: Sexuelle Themen werden normalisiert ($confidencePercent% Konfidenz)"
            "SEXUAL_CONTENT" -> "Explizite sexuelle Inhalte erkannt ($confidencePercent% Konfidenz)"
            "MAINTENANCE" -> "Geheimhaltungs-Phase: Versuch, Schweigen zu erzwingen ($confidencePercent% Konfidenz)"
            else -> "Unbekannte Stage: $stage"
        }
    }

    override fun close() {
        interpreter?.close()
        interpreter = null
        Log.d(TAG, "🔒 Osprey Detector geschlossen")
    }

    /**
     * Analysiert eine komplette Konversation statt nur einer einzelnen Nachricht
     *
     * Dies ist die Hauptmethode für die Osprey-Integration.
     * Erkennt Grooming-Muster durch Eskalation über mehrere Nachrichten.
     *
     * @param messages Liste von Konversationsnachrichten
     * @param contextFeatures Optionale kontextuelle Features aus ConversationBuffer
     * @return OspreyResult mit Stage-Erkennung basierend auf Konversationsverlauf
     */
    fun analyzeConversation(
        messages: List<ConversationBuffer.ConversationMessage>,
        contextFeatures: Map<String, Float> = emptyMap()
    ): OspreyResult {
        if (messages.isEmpty()) {
            return OspreyResult(
                stage = "SAFE",
                confidence = 1.0f,
                isRisk = false,
                allStageScores = mapOf("SAFE" to 1.0f),
                explanation = "Keine Nachrichten zur Analyse",
                contextFeatures = contextFeatures,
                conversationLength = 0
            )
        }

        // 1. Konversation formatieren mit Autor-Tags
        val formattedConversation = formatConversationForOsprey(messages)

        // 2. Analysiere jede Nachricht einzeln und sammle Stages
        val messageStages = mutableListOf<Pair<String, Float>>()
        val contactMessages = messages.filter { !it.isLocalUser }

        for (msg in contactMessages) {
            val result = predict(msg.text)
            if (result != null) {
                messageStages.add(result.stage to result.confidence)
            }
        }

        // 3. Analysiere die gesamte formatierte Konversation
        val fullResult = predict(formattedConversation)

        // 4. Berechne Stage-Progression
        val stageProgression = analyzeStageProgression(messageStages)

        // 5. Kombiniere Ergebnisse
        val finalStage: String
        val finalConfidence: Float
        val isRisk: Boolean

        if (fullResult != null && fullResult.isRisk) {
            // Modell hat ein Risiko in der Gesamtkonversation erkannt
            finalStage = fullResult.stage
            finalConfidence = fullResult.confidence
            isRisk = true
        } else if (stageProgression.isProgressing) {
            // Progression durch Stages erkannt (Trust → Isolation → ...)
            finalStage = stageProgression.currentStage
            finalConfidence = stageProgression.progressionConfidence
            isRisk = stageProgression.isRisky
        } else if (messageStages.isNotEmpty()) {
            // Fallback: Höchste erkannte Stage
            val highestRisk = messageStages
                .filter { it.first != "SAFE" }
                .maxByOrNull { STAGES.indexOf(it.first) }

            if (highestRisk != null) {
                finalStage = highestRisk.first
                finalConfidence = highestRisk.second
                isRisk = true
            } else {
                finalStage = "SAFE"
                finalConfidence = 1.0f
                isRisk = false
            }
        } else {
            finalStage = fullResult?.stage ?: "SAFE"
            finalConfidence = fullResult?.confidence ?: 1.0f
            isRisk = fullResult?.isRisk ?: false
        }

        // 6. Generiere Erklärung mit Konversationskontext
        val explanation = generateConversationExplanation(
            stage = finalStage,
            confidence = finalConfidence,
            messageCount = messages.size,
            progression = stageProgression
        )

        Log.d(TAG, "🔍 Conversation Analysis: $finalStage (${(finalConfidence * 100).toInt()}%), ${messages.size} msgs")

        return OspreyResult(
            stage = finalStage,
            confidence = finalConfidence,
            isRisk = isRisk,
            allStageScores = fullResult?.allStageScores ?: mapOf(finalStage to finalConfidence),
            explanation = explanation,
            contextFeatures = contextFeatures,
            conversationLength = messages.size,
            dominantProgression = stageProgression.progressionPath
        )
    }

    /**
     * Formatiert Nachrichten für Osprey-Input
     */
    private fun formatConversationForOsprey(messages: List<ConversationBuffer.ConversationMessage>): String {
        return messages.joinToString(" [SEP] ") { msg ->
            val authorTag = if (msg.isLocalUser) "[CHILD]" else "[CONTACT]"
            "$authorTag ${msg.text}"
        }
    }

    /**
     * Analysiert die Stage-Progression über den Konversationsverlauf
     */
    private data class StageProgressionResult(
        val isProgressing: Boolean,
        val isRisky: Boolean,
        val currentStage: String,
        val progressionConfidence: Float,
        val progressionPath: String?
    )

    private fun analyzeStageProgression(stages: List<Pair<String, Float>>): StageProgressionResult {
        if (stages.size < 2) {
            return StageProgressionResult(
                isProgressing = false,
                isRisky = false,
                currentStage = stages.firstOrNull()?.first ?: "SAFE",
                progressionConfidence = stages.firstOrNull()?.second ?: 1.0f,
                progressionPath = null
            )
        }

        // Filtere SAFE-Stages raus
        val riskStages = stages.filter { it.first != "SAFE" }

        if (riskStages.isEmpty()) {
            return StageProgressionResult(
                isProgressing = false,
                isRisky = false,
                currentStage = "SAFE",
                progressionConfidence = 1.0f,
                progressionPath = null
            )
        }

        // Prüfe auf Progression (höhere Stage-Indices = gefährlicher)
        val stageIndices = riskStages.map { STAGES.indexOf(it.first) }
        val isProgressing = stageIndices.zipWithNext().any { (a, b) -> b > a }

        // Aktuelle (letzte/höchste) Stage
        val currentStageIdx = stageIndices.maxOrNull() ?: 0
        val currentStage = STAGES.getOrElse(currentStageIdx) { "SAFE" }

        // Durchschnittliche Confidence der Risk-Stages
        val avgConfidence = riskStages.map { it.second }.average().toFloat()

        // Progression-Pfad für Logging
        val progressionPath = riskStages.map { it.first }.distinct().joinToString(" → ")

        // Risiko wenn: mehrere Stages ODER höhere Stages erreicht
        val isRisky = riskStages.size >= 2 || currentStageIdx >= 2  // ISOLATION oder höher

        return StageProgressionResult(
            isProgressing = isProgressing,
            isRisky = isRisky,
            currentStage = currentStage,
            progressionConfidence = avgConfidence,
            progressionPath = progressionPath
        )
    }

    /**
     * Generiert Erklärung mit Konversationskontext
     */
    private fun generateConversationExplanation(
        stage: String,
        confidence: Float,
        messageCount: Int,
        progression: StageProgressionResult
    ): String {
        val confidencePercent = (confidence * 100).toInt()
        val baseExplanation = generateExplanation(stage, confidence)

        return buildString {
            append(baseExplanation)

            if (messageCount > 1) {
                append("\n\n📊 Konversationsanalyse:")
                append("\n• $messageCount Nachrichten analysiert")

                if (progression.isProgressing && progression.progressionPath != null) {
                    append("\n• ⚠️ Eskalation erkannt: ${progression.progressionPath}")
                }
            }
        }
    }

    /**
     * Vereinfachter Tokenizer (für BERT-ähnliche Modelle)
     *
     * Hinweis: Für Production sollte ein echter BERT-Tokenizer verwendet werden
     * (z.B. aus HuggingFace transformers)
     */
    private class SimpleTokenizer {
        private val vocabulary = mutableMapOf<String, Int>()
        private val CLS_TOKEN = 101  // [CLS] token
        private val SEP_TOKEN = 102  // [SEP] token
        private val PAD_TOKEN = 0    // [PAD] token
        private val UNK_TOKEN = 100  // [UNK] token

        init {
            // Basic vocabulary (würde normalerweise aus vocab.txt geladen)
            buildBasicVocab()
        }

        fun tokenize(text: String, maxLength: Int): List<Int> {
            val tokens = mutableListOf<Int>()

            // [CLS] am Anfang
            tokens.add(CLS_TOKEN)

            // Tokenize words
            val words = text.lowercase()
                .replace(Regex("[^a-z0-9äöüß\\s]"), " ")
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }

            for (word in words) {
                if (tokens.size >= maxLength - 1) break  // Reserve space for [SEP]

                val tokenId = vocabulary[word] ?: UNK_TOKEN
                tokens.add(tokenId)
            }

            // [SEP] am Ende
            if (tokens.size < maxLength) {
                tokens.add(SEP_TOKEN)
            }

            return tokens
        }

        private fun buildBasicVocab() {
            // Grooming-relevante Keywords (vereinfacht)
            val keywords = listOf(
                // Trust Building
                "hübsch", "schön", "süß", "nett", "lieb", "toll", "perfekt",
                "pretty", "beautiful", "sweet", "nice", "perfect",

                // Isolation
                "allein", "alleine", "geheim", "niemand", "secret", "alone",
                "eltern", "parents", "freunde", "friends",

                // Desensitization
                "körper", "body", "aussehen", "look", "bild", "picture", "foto", "photo",

                // Assessment
                "zimmer", "room", "wo", "where", "wann", "when",

                // Gift Giving
                "geld", "money", "kaufen", "buy", "geschenk", "gift"
            )

            keywords.forEachIndexed { idx, word ->
                vocabulary[word] = 1000 + idx  // Start at 1000
            }
        }
    }
}
