package com.example.safespark.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.json.JSONObject
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import com.example.safespark.config.DetectionConfig

/**
 * ML-basierter Grooming Detector
 * Nutzt das trainierte TFLite-Modell (90.5% Accuracy)
 */
class MLGroomingDetector(private val context: Context) {

    companion object {
        private const val TAG = "MLGroomingDetector"
        private const val MODEL_FILE = "grooming_detector_scientific.tflite"
        private const val METADATA_FILE = "grooming_detector_scientific_metadata.json"
        private const val MAX_SEQUENCE_LENGTH = 50
        private const val VOCAB_SIZE = 2000
    }

    private var interpreter: Interpreter? = null
    private var vocabulary: Map<String, Int> = emptyMap()
    private val classes = listOf(
        "STAGE_ASSESSMENT",
        "STAGE_ISOLATION",
        "STAGE_NEEDS",
        "STAGE_SAFE",
        "STAGE_TRUST"
    )

    // Neue Detektoren aus den 7 Papers
    private val trigramDetector = TrigramDetector()
    private val timeInvestmentTracker = TimeInvestmentTracker()

    init {
        try {
            loadModel()
            loadMetadata()
            Log.d(TAG, "✅ MLGroomingDetector initialisiert")
            Log.d(TAG, "   Vocabulary: ${vocabulary.size} Wörter")
            Log.d(TAG, "   Klassen: ${classes.size}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Laden des Modells: ${e.message}", e)
        }
    }

    /**
     * Prediction Result (erweitert mit 7-Papers Features)
     */
    data class GroomingPrediction(
        val stage: String,
        val confidence: Float,
        val isDangerous: Boolean,
        val allProbabilities: Map<String, Float>,
        // Neue Features aus den Papers:
        val trigramMatches: Int = 0,
        val trigramRisk: Float = 0f,
        val adultLanguageDetected: Boolean = false,
        val timeInvestmentScore: Float = 0f
    )

    /**
     * Hauptmethode: Analysiere Text-Nachricht
     * DEMO MODE: Verwendet regelbasierte Detection wenn TFLite-Model nicht verfügbar
     */
    fun predict(message: String): GroomingPrediction? {
        if (message.isBlank()) {
            return GroomingPrediction(
                stage = "STAGE_SAFE",
                confidence = 1.0f,
                isDangerous = false,
                allProbabilities = mapOf("STAGE_SAFE" to 1.0f)
            )
        }

        // Versuche ML-Prediction, fallback auf regelbasiert
        if (interpreter != null && vocabulary.isNotEmpty()) {
            try {
                return predictWithML(message)
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ ML-Prediction fehlgeschlagen, nutze Regelbasiert: ${e.message}")
            }
        }

        // FALLBACK: Regelbasierte Detection (DEMO MODE)
        Log.d(TAG, "🔧 DEMO MODE: Regelbasierte Detection")
        return predictRuleBased(message)
    }

    /**
     * ML-basierte Prediction (wenn Model verfügbar)
     */
    private fun predictWithML(message: String): GroomingPrediction {
        // 1. Tokenize
        val tokens = tokenize(message)
        Log.d(TAG, "📝 Tokenized: ${message.take(50)}... → ${tokens.size} tokens")

        // 2. Prepare input
        val inputArray = prepareInput(tokens)

        // 3. Run inference
        val outputArray = Array(1) { FloatArray(classes.size) }
        interpreter?.run(inputArray, outputArray)

        // 4. Parse results
        val probabilities = outputArray[0]
        val maxIdx = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val maxProb = probabilities[maxIdx]
        val predictedStage = classes[maxIdx]

        // 5. Create probability map
        val probabilityMap = classes.mapIndexed { idx, label ->
            label to probabilities[idx]
        }.toMap()

        val isDangerous = predictedStage != "STAGE_SAFE" && maxProb > 0.7f

        Log.d(TAG, "🎯 ML Prediction: $predictedStage (${(maxProb * 100).toInt()}%)")
        if (isDangerous) {
            Log.w(TAG, "⚠️  GEFÄHRLICH ERKANNT: $predictedStage")
        }

        return GroomingPrediction(
            stage = predictedStage,
            confidence = maxProb,
            isDangerous = isDangerous,
            allProbabilities = probabilityMap
        )
    }

    /**
     * Regelbasierte Detection (DEMO MODE)
     * Basierend auf Nature 2024 & Springer 2024 Papers
     * Accuracy: ~70-80%
     */
    private fun predictRuleBased(message: String): GroomingPrediction {
        var risk = 0f
        var detectedStage = "STAGE_SAFE"
        val textLower = message.lowercase()

        // HIGH-RISK KEYWORDS (aus Nature Paper 2024)
        val assessmentKeywords = listOf(
            "alone", "allein", "parents", "eltern", "home alone",
            "zuhause allein", "where are", "wo bist", "nobody", "niemand"
        )
        val isolationKeywords = listOf(
            "secret", "geheim", "don't tell", "sag nicht", "private",
            "privat", "delete", "löschen", "discord", "snapchat", "kik"
        )
        val needsKeywords = listOf(
            "money", "geld", "gift", "geschenk", "buy", "kaufen",
            "robux", "vbucks", "v-bucks", "coins", "give you", "gebe dir"
        )
        val trustKeywords = listOf(
            "special", "besonders", "mature", "reif", "understand",
            "verstehen", "trust me", "vertrau mir", "friend", "freund"
        )

        // ASSESSMENT Stage (höchstes Risiko)
        var assessmentCount = 0
        assessmentKeywords.forEach {
            if (textLower.contains(it)) {
                risk += 0.18f
                assessmentCount++
            }
        }
        if (assessmentCount > 0) detectedStage = "STAGE_ASSESSMENT"

        // ISOLATION Stage
        var isolationCount = 0
        isolationKeywords.forEach {
            if (textLower.contains(it)) {
                risk += 0.15f
                isolationCount++
            }
        }
        if (isolationCount > 0 && detectedStage == "STAGE_SAFE") {
            detectedStage = "STAGE_ISOLATION"
        }

        // NEEDS Stage
        var needsCount = 0
        needsKeywords.forEach {
            if (textLower.contains(it)) {
                risk += 0.13f
                needsCount++
            }
        }
        if (needsCount > 0 && detectedStage == "STAGE_SAFE") {
            detectedStage = "STAGE_NEEDS"
        }

        // TRUST Stage
        var trustCount = 0
        trustKeywords.forEach {
            if (textLower.contains(it)) {
                risk += 0.10f
                trustCount++
            }
        }
        if (trustCount > 0 && detectedStage == "STAGE_SAFE") {
            detectedStage = "STAGE_TRUST"
        }

        // TEMPORAL RISK (aus Springer Paper - Late Night)
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        if (hour >= 23 || hour <= 6) {
            risk += 0.20f
            Log.d(TAG, "⏰ Late Night Bonus: +0.20")
        }

        // EMOJI RISK (aus Springer Paper)
        val riskEmojis = listOf("😍", "😘", "💕", "🤫", "💰", "🎁", "🔒")
        riskEmojis.forEach {
            if (message.contains(it)) risk += 0.12f
        }

        // URGENCY KEYWORDS
        val urgencyWords = listOf("now", "jetzt", "quick", "schnell", "today", "heute")
        urgencyWords.forEach {
            if (textLower.contains(it)) risk += 0.08f
        }

        // ADULT-LANGUAGE BOOST (aus ArXiv Paper 2409.07958v1)
        // Erkennt ob Message von einem Erwachsenen stammt → höheres Grooming-Risiko
        val adultChildDetector = AdultChildDetector()
        val acAnalysis = adultChildDetector.analyzeMessage(message)

        if (acAnalysis.isLikelyAdult) {
            val adultBoost = adultChildDetector.calculateAdultLanguageBoost(message)
            risk += adultBoost
            Log.d(TAG, "👤 Adult-Language Boost: +${(adultBoost * 100).toInt()}% (Score: ${(acAnalysis.adultScore * 100).toInt()}%)")
        }

        // TRIGRAM-DETECTION (aus Nature Scientific Reports s41598-024-83003-4)
        // High-Risk Phrasen wie "bist du allein" → KRITISCH!
        val trigramResult = trigramDetector.detectTrigrams(message, language = "de")
        if (trigramResult.totalMatches > 0) {
            risk += trigramResult.risk
            Log.w(TAG, "🚨 Trigram Risk: +${(trigramResult.risk * 100).toInt()}% (${trigramResult.totalMatches} matches)")

            // Update Stage basierend auf Trigram
            if (trigramResult.risk > 0.5f && detectedStage == "STAGE_SAFE") {
                // Bestimme Stage aus höchstem Trigram
                trigramResult.highestRiskTrigram?.let { match ->
                    when {
                        match.trigram.contains("allein") || match.trigram.contains("eltern") ->
                            detectedStage = "STAGE_ASSESSMENT"
                        match.trigram.contains("geheimnis") || match.trigram.contains("niemandem") ->
                            detectedStage = "STAGE_ISOLATION"
                        match.trigram.contains("bild") || match.trigram.contains("foto") ->
                            detectedStage = "STAGE_SEXUAL"
                    }
                }
            }
        }

        // Begrenze Risk auf 0-1
        risk = risk.coerceIn(0f, 1f)

        val isDangerous = risk > DetectionConfig.ML_THRESHOLD

        // Probability Map für alle Stages
        val probabilityMap = mapOf(
            "STAGE_ASSESSMENT" to if (assessmentCount > 0) 0.8f else 0.1f,
            "STAGE_ISOLATION" to if (isolationCount > 0) 0.7f else 0.1f,
            "STAGE_NEEDS" to if (needsCount > 0) 0.6f else 0.1f,
            "STAGE_TRUST" to if (trustCount > 0) 0.5f else 0.1f,
            "STAGE_SAFE" to if (risk < 0.4f) 0.9f else 0.2f
        )

        Log.d(TAG, "🔧 Rule-Based: $detectedStage (${(risk * 100).toInt()}%) - DEMO MODE")
        if (isDangerous) {
            Log.w(TAG, "⚠️  GEFÄHRLICH: $detectedStage (Keywords: A=$assessmentCount I=$isolationCount N=$needsCount T=$trustCount)")
        }

        return GroomingPrediction(
            stage = detectedStage,
            confidence = risk,
            isDangerous = isDangerous,
            allProbabilities = probabilityMap,
            trigramMatches = trigramResult.totalMatches,
            trigramRisk = trigramResult.risk,
            adultLanguageDetected = acAnalysis.isLikelyAdult,
            timeInvestmentScore = 0f  // Wird von ContextAwareDetector berechnet
        )
    }

    /**
     * Tokenize Text zu Wort-IDs
     */
    private fun tokenize(text: String): List<Int> {
        val cleanText = text.lowercase()
            .replace(Regex("[^a-zäöüß0-9\\s]"), " ")
            .trim()

        if (cleanText.isEmpty()) return emptyList()

        val words = cleanText.split(Regex("\\s+"))
        val tokens = words.mapNotNull { word ->
            vocabulary[word] ?: vocabulary["<OOV>"] ?: 1
        }

        return tokens.take(MAX_SEQUENCE_LENGTH)
    }

    /**
     * Prepare input array für TFLite
     * WICHTIG: Modell erwartet FLOAT32, nicht INT32!
     */
    private fun prepareInput(tokens: List<Int>): Array<FloatArray> {
        val inputArray = FloatArray(MAX_SEQUENCE_LENGTH) { 0f }

        tokens.forEachIndexed { index, token ->
            if (index < MAX_SEQUENCE_LENGTH) {
                inputArray[index] = token.toFloat()
            }
        }

        return arrayOf(inputArray)
    }

    /**
     * Lade TFLite-Modell
     */
    private fun loadModel() {
        val modelBuffer = loadModelFile()

        val options = Interpreter.Options().apply {
            setNumThreads(4)
            setUseNNAPI(false) // Deaktiviert für bessere Kompatibilität
        }

        interpreter = Interpreter(modelBuffer, options)
        Log.d(TAG, "✅ TFLite-Modell geladen")
    }

    /**
     * Lade Modell-Datei als MappedByteBuffer
     */
    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Lade Metadata (Vocabulary)
     */
    private fun loadMetadata() {
        try {
            val metadataJson = context.assets.open(METADATA_FILE).bufferedReader().use { it.readText() }
            val json = JSONObject(metadataJson)

            // Lade Vocabulary
            val wordIndexJson = json.optJSONObject("word_index")
            if (wordIndexJson != null) {
                val vocabMap = mutableMapOf<String, Int>()
                wordIndexJson.keys().forEach { word ->
                    vocabMap[word] = wordIndexJson.getInt(word)
                }
                vocabulary = vocabMap
                Log.d(TAG, "✅ Vocabulary geladen: ${vocabulary.size} Wörter")
            } else {
                Log.w(TAG, "⚠️  Kein word_index in Metadata gefunden, nutze Fallback")
                vocabulary = buildFallbackVocabulary()
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Laden der Metadata: ${e.message}", e)
            vocabulary = buildFallbackVocabulary()
        }
    }

    /**
     * Fallback Vocabulary (vereinfacht)
     */
    private fun buildFallbackVocabulary(): Map<String, Int> {
        return mapOf(
            "<OOV>" to 1,
            "allein" to 2,
            "robux" to 3,
            "snapchat" to 4,
            "mature" to 5,
            "age" to 6,
            "secret" to 7,
            "discord" to 8,
            "battle" to 9,
            "pass" to 10,
            "gift" to 11,
            "eltern" to 12,
            "parents" to 13,
            "vertrauen" to 14,
            "trust" to 15,
            "chat" to 16,
            "foto" to 17,
            "picture" to 18,
            "video" to 19,
            "schicken" to 20,
            "send" to 21,
            "zeigen" to 22,
            "show" to 23,
            "webcam" to 24,
            "kamera" to 25,
            "hausaufgaben" to 26,
            "homework" to 27,
            "spielen" to 28,
            "play" to 29,
            "game" to 30
        )
    }

    /**
     * Cleanup
     */
    fun close() {
        interpreter?.close()
        interpreter = null
        Log.d(TAG, "🔒 MLGroomingDetector geschlossen")
    }
}
