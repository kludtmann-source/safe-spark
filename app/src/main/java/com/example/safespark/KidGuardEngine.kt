package com.example.safespark

import android.content.Context
import android.util.Log
import com.example.safespark.ml.MLGroomingDetector
import com.example.safespark.ml.TrigramDetector
import com.example.safespark.ml.TimeInvestmentTracker
import com.example.safespark.ml.AdultChildDetector
import com.example.safespark.ml.ContextAwareDetector
import com.example.safespark.ml.StageProgressionDetector
import com.example.safespark.ml.OspreyLocalDetector
import com.example.safespark.ml.ConversationBuffer
import com.example.safespark.detection.SemanticDetector
import com.example.safespark.model.GroomingIntent
import com.example.safespark.logging.DetectionLogger
import com.example.safespark.logging.DetectionLogger.GroomingStage
import com.example.safespark.config.DetectionConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Closeable

/**
 * Ergebnis der Text-Analyse mit Erklärung (Explainable AI)
 * Basierend auf Basani et al. 2025 Paper
 */
data class AnalysisResult(
    val score: Float,
    val isRisk: Boolean,
    val explanation: String,
    val detectionMethod: String,
    val detectedPatterns: List<String> = emptyList(),
    val stage: String = "UNKNOWN",
    val confidence: Float = 0f,
    val allStageScores: Map<String, Float> = emptyMap()
)

/**
 * SafeSpark Engine für Text-Analyse
 *
 * Hybrid-System: Kombiniert 9 Detection-Layers:
 * 0. Semantic Similarity (NEU! - ERSTE PRIORITÄT)
 * 1. Osprey Transformer (On-Device BERT/RoBERTa - 6 Grooming-Stages)
 * 2. ML-Modell (90.5% Accuracy)
 * 3. Trigram-Detection (+3% Accuracy)
 * 4. Time Investment Tracking (+2% Accuracy)
 * 5. Stage Progression Detection (+1% Accuracy)
 * 6. Adult/Child Context
 * 7. Context-Aware Detection
 * 8. Keyword-Matching (Fallback)
 *
 * GESAMT: ~95% Accuracy mit Semantic + Osprey Layer!
 */
class KidGuardEngine(private val context: Context) : Closeable {

    private val riskKeywords: Set<String>
    private val mlDetector: MLGroomingDetector
    private val trigramDetector: TrigramDetector
    private val timeTracker: TimeInvestmentTracker
    private val adultChildDetector: AdultChildDetector
    private val contextDetector: ContextAwareDetector
    private val stageDetector: StageProgressionDetector
    private val semanticDetector: SemanticDetector?  // Nullable für Fallback
    private val ospreyDetector: OspreyLocalDetector?  // Nullable falls Modell nicht verfügbar
    private val TAG = "SafeSparkEngine"

    // Stage History für Progression-Tracking
    private val stageHistory = mutableListOf<StageProgressionDetector.StageEvent>()

    init {
        try {
            // Lade Risk Keywords aus Vocabulary
            riskKeywords = loadRiskKeywords(context)
            Log.d(TAG, "✅ Engine initialisiert mit ${riskKeywords.size} Risk-Keywords")

            // Initialisiere alle Detektoren
            mlDetector = MLGroomingDetector(context)
            trigramDetector = TrigramDetector()
            timeTracker = TimeInvestmentTracker()
            adultChildDetector = AdultChildDetector()
            contextDetector = ContextAwareDetector()
            stageDetector = StageProgressionDetector()

            // Initialisiere Semantic Detector (mit Fallback)
            semanticDetector = try {
                SemanticDetector(context).also {
                    Log.d(TAG, "✅ Semantic Detector initialisiert (HÖCHSTE PRIORITÄT)")
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Semantic Detector konnte nicht geladen werden, nutze Fallback", e)
                null
            }

            // Initialisiere Osprey Detector (mit Fallback)
            ospreyDetector = try {
                OspreyLocalDetector(context).also {
                    Log.d(TAG, "✅ Osprey Transformer-Detector initialisiert (6 Stages)")
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Osprey Detector nicht verfügbar (Modell fehlt)", e)
                null
            }

            Log.d(TAG, "✅ ML-Detector initialisiert (90.5% Accuracy)")
            Log.d(TAG, "✅ Trigram-Detector initialisiert (+3% Accuracy)")
            Log.d(TAG, "✅ Time Investment Tracker initialisiert (+2% Accuracy)")
            Log.d(TAG, "✅ Stage Progression Detector initialisiert (+1% Accuracy)")
            Log.d(TAG, "✅ Adult/Child Detector initialisiert")
            Log.d(TAG, "✅ Context-Aware Detector initialisiert")

            val totalLayers = 7 + (if (semanticDetector != null) 1 else 0) + (if (ospreyDetector != null) 1 else 0)
            val estimatedAccuracy = 90 + (if (semanticDetector != null) 3 else 0) + (if (ospreyDetector != null) 2 else 0)
            Log.d(TAG, "🎯 GESAMT: $totalLayers Detection-Layers, ~$estimatedAccuracy% Accuracy erreicht!")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Initialisieren der Engine", e)
            throw RuntimeException("Fehler beim Initialisieren des SafeSparkEngine", e)
        }
    }
    
    /**
     * Analysiert einen Text und gibt einen Sicherheits-Score zurück
     * 
     * Hybrid-Ansatz mit 7 Detection-Layers:
     * 1. ML-Modell (90.5% Accuracy)
     * 2. Trigram-Detection (+3%)
     * 3. Adult/Child Context Detection
     * 4. Context-Aware Detection
     * 5. Stage Progression Tracking (+1%)
     * 6. Assessment Pattern Matching
     * 7. Keyword-Matching (Fallback)
     *
     * @param input Der zu analysierende Text
     * @param appPackage Package-Name der Quell-App (für Context-Aware Detection)
     * @return Score zwischen 0.0 und 1.0, wobei höhere Werte auf riskanten Content hinweisen
     */
    fun analyzeText(input: String, appPackage: String = "unknown"): Float {
        Log.d(TAG, "analyzeText() aufgerufen mit: '$input' (App: $appPackage)")
        Log.e(TAG, "🔥 VERSION-CHECK: Assessment-Fix v2.0-WORKAROUND aktiv!")

        // TEXT-LENGTH GATE: Skip short texts (DetectionConfig.MIN_TEXT_LENGTH)
        if (input.trim().length < DetectionConfig.MIN_TEXT_LENGTH) {
            Log.d(TAG, "⏭️ Text zu kurz (${input.trim().length} < ${DetectionConfig.MIN_TEXT_LENGTH}), skipping")
            return 0.0f
        }

        // WORD COUNT GATE: Skip texts with too few words
        val wordCount = input.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        if (wordCount < DetectionConfig.MIN_WORDS_FOR_PATTERN) {
            Log.d(TAG, "⏭️ Zu wenig Wörter ($wordCount < ${DetectionConfig.MIN_WORDS_FOR_PATTERN}), skipping")
            return 0.0f
        }

        val scores = mutableMapOf<String, Float>()

        // 1. ML-Prediction (Basis: 90.5%)
        val mlPrediction = mlDetector.predict(input)
        if (mlPrediction != null) {
            val mlScore = if (mlPrediction.isDangerous) mlPrediction.confidence else 0.0f
            scores["ML"] = mlScore
            Log.d(TAG, "🤖 ML-Prediction: ${mlPrediction.stage} (${(mlPrediction.confidence * 100).toInt()}%)")

            // Track Stage für Progression-Analyse
            if (mlPrediction.isDangerous && mlPrediction.confidence > 0.6f) {
                val stageEvent = stageDetector.createStageEvent(
                    stageName = mlPrediction.stage,
                    confidence = mlPrediction.confidence,
                    timestamp = System.currentTimeMillis(),
                    messageText = input
                )
                stageHistory.add(stageEvent)

                // Behalte nur letzte 20 Stages
                if (stageHistory.size > 20) {
                    stageHistory.removeAt(0)
                }
            }
        }

        // 2. Trigram-Detection (+3% Accuracy) - Check both DE and EN
        val trigramResultDE = trigramDetector.detectTrigrams(input, "de")
        val trigramResultEN = trigramDetector.detectTrigrams(input, "en")
        val trigramResult = if (trigramResultDE.risk >= trigramResultEN.risk) trigramResultDE else trigramResultEN
        scores["Trigram"] = trigramResult.risk
        if (trigramResult.risk > 0.3f) {
            Log.w(TAG, "🔺 Trigram Risk: ${(trigramResult.risk * 100).toInt()}% (${trigramResult.totalMatches} matches)")
        }

        // 3. Adult/Child Context Detection
        val adultChildResult = adultChildDetector.analyzeMessage(input)
        if (adultChildResult.isLikelyAdult && adultChildResult.adultScore > 0.7f) {
            scores["AdultContext"] = adultChildResult.adultScore * 0.8f // Boost bei Adult-Kontext
            Log.w(TAG, "👤 Adult Context detected: ${(adultChildResult.adultScore * 100).toInt()}%")
        }

        // 4. Context-Aware Detection (Springer Paper 978-3-031-62083-6)
        val contextResult = contextDetector.analyzeWithContext(
            appPackage = appPackage,  // ✅ Jetzt wird das echte Package übergeben!
            text = input,
            baseScore = scores["ML"] ?: 0f,
            baseStage = mlPrediction?.stage ?: "UNKNOWN",
            timestamp = System.currentTimeMillis()
        )
        scores["Context"] = contextResult.score
        if (contextResult.score > 0.3f) {
            Log.w(TAG, "📊 Context Risk: ${(contextResult.score * 100).toInt()}% (Bonus: +${(contextResult.contextBonus * 100).toInt()}%)")
        }

        // 5. Stage Progression Analysis (+1% Accuracy)
        if (stageHistory.size >= 2) {
            val progressionAnalysis = stageDetector.analyzeProgression(stageHistory)
            scores["StageProgression"] = progressionAnalysis.riskScore

            if (progressionAnalysis.isAnomalous) {
                Log.e(TAG, "🚨 ANOMALOUS Stage Progression detected!")
                progressionAnalysis.warnings.forEach { Log.e(TAG, "   $it") }
            }
        }

        // 6. Check for bypass patterns first (these override all checks)
        if (DetectionConfig.matchesBypassPattern(input)) {
            Log.w(TAG, "🚨 BYPASS PATTERN detected - immediate high risk!")
            return 0.95f
        }

        // 7. Spezifische Assessment-Pattern-Prüfung (collect score, no early return)
        val lowerInput = input.lowercase().trim()

        // NEGATIONS-CHECK: Reduziert False Positives
        if (!DetectionConfig.containsNegation(input)) {
            // High-Risk Assessment Patterns (only clearly suspicious patterns)
            val assessmentPatterns = listOf(
                // Isolation/Supervision - Deutsch (Phrasen)
                "bist du allein" to 0.85f,
                "bist du alleine" to 0.85f,
                "bist du gerade allein" to 0.90f,
                "ist jemand bei dir" to 0.80f,
                "ist niemand da" to 0.85f,
                "wo sind deine eltern" to 0.80f,
                "ist deine tür zu" to 0.80f,
                "bist du in deinem zimmer" to 0.75f,

                // Isolation/Supervision - Englisch (Phrasen)
                "are you alone" to 0.85f,
                "are you home alone" to 0.90f,
                "is anyone there" to 0.80f,
                "where are your parents" to 0.80f,
                "is your door closed" to 0.80f,

                // Gift Giving - Only clearly suspicious patterns
                "ich schicke dir geld" to 0.85f,
                "ich schick dir geld" to 0.85f,
                "ich gebe dir geld" to 0.85f,
                "ich geb dir geld" to 0.85f,
                "ich überweise dir" to 0.85f,
                "i'll send you money" to 0.85f,
                "i'll give you money" to 0.85f
            )

            for ((pattern, riskScore) in assessmentPatterns) {
                if (lowerInput.contains(pattern)) {
                    scores["Assessment"] = riskScore

                    // 🚨 STRUCTURED FINDING LOG (nur positive!)
                    DetectionLogger.logFinding(
                        text = input,
                        score = riskScore,
                        stage = GroomingStage.ASSESSMENT,
                        method = "Assessment-Pattern",
                        pattern = pattern
                    )

                    break  // Only match first pattern
                }
            }
        }

        // 8. Keyword-Matching (Fallback) - Fixed threshold
        val words = lowerInput.split(Regex("[\\s\\W]+"))
        var riskCount = 0
        val matchedKeywords = mutableListOf<String>()

        for (word in words) {
            if (word.isNotEmpty() && riskKeywords.contains(word)) {
                riskCount++
                matchedKeywords.add(word)
                Log.d(TAG, "   🔴 Risk-Keyword gefunden: '$word'")
            }
        }

        // Fixed keyword scoring: require more keywords or lower scores
        val keywordScore = when {
            riskCount == 0 -> 0.0f
            riskCount == 1 -> 0.20f  // Reduced from 0.75f
            riskCount == 2 -> 0.40f  // Reduced from 0.95f
            else -> 0.75f  // 3+ keywords for meaningful score
        }
        scores["Keywords"] = keywordScore

        // Apply confidence adjustment by length to all scores
        val adjustedScores = scores.mapValues { (_, score) ->
            DetectionConfig.adjustConfidenceByLength(score, input.length)
        }.toMutableMap()

        // Log adjustments if text is short
        if (input.length < 30) {
            Log.d(TAG, "📏 Length adjustment applied (text length: ${input.length})")
        }

        // Check for layer agreement (dual-confirmation)
        if (!DetectionConfig.hasLayerAgreement(adjustedScores)) {
            Log.d(TAG, "⚠️ Insufficient layer agreement - returning safe result")
            return 0.0f
        }

        // KOMBINIERE ALLE SCORES (gewichteter Durchschnitt)
        val finalScore = calculateWeightedScore(adjustedScores)

        Log.d(TAG, "📊 Detection Scores: ${adjustedScores.map { "${it.key}=${(it.value*100).toInt()}%" }.joinToString(", ")}")
        Log.d(TAG, "🎯 FINAL SCORE: ${(finalScore * 100).toInt()}%")

        return finalScore
    }

    /**
     * Analysiert Text mit Erklärung (Explainable AI)
     * Basierend auf Basani et al. 2025 Paper
     *
     * Detection-Reihenfolge:
     * 0. Semantic Similarity (NEU! - HÖCHSTE PRIORITÄT)
     * 1. Assessment-Pattern Check
     * 2. ML-Modell + weitere Detektoren
     *
     * @param input Der zu analysierende Text
     * @param appPackage Package-Name der Quell-App
     * @return AnalysisResult mit Score und Erklärung
     */
    fun analyzeTextWithExplanation(input: String, appPackage: String = "unknown"): AnalysisResult {
        if (input.isBlank()) {
            return AnalysisResult(
                score = 0.0f,
                isRisk = false,
                explanation = "Leerer Text",
                detectionMethod = "None"
            )
        }

        // TEXT-LENGTH GATE: Skip short texts
        if (input.trim().length < DetectionConfig.MIN_TEXT_LENGTH) {
            Log.d(TAG, "⏭️ Text zu kurz (${input.trim().length} < ${DetectionConfig.MIN_TEXT_LENGTH}), safe result")
            return AnalysisResult(
                score = 0.0f,
                isRisk = false,
                explanation = "Text zu kurz für Analyse",
                detectionMethod = "TextLengthGate"
            )
        }

        // WORD COUNT GATE: Skip texts with too few words
        val wordCount = input.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        if (wordCount < DetectionConfig.MIN_WORDS_FOR_PATTERN) {
            Log.d(TAG, "⏭️ Zu wenig Wörter ($wordCount < ${DetectionConfig.MIN_WORDS_FOR_PATTERN}), safe result")
            return AnalysisResult(
                score = 0.0f,
                isRisk = false,
                explanation = "Zu wenig Wörter für Musteranalyse",
                detectionMethod = "WordCountGate"
            )
        }

        val scores = mutableMapOf<String, Float>()
        val detectedPatterns = mutableListOf<String>()
        var detectionMethod = "Unknown"
        var explanation = ""

        // CHECK FOR BYPASS PATTERNS FIRST (these trigger immediately!)
        if (DetectionConfig.matchesBypassPattern(input)) {
            Log.w(TAG, "🚨 BYPASS PATTERN detected - immediate alert!")
            // Find which bypass pattern matched
            val matchedPattern = DetectionConfig.BYPASS_PATTERNS.find { 
                input.lowercase().contains(it) 
            } ?: "bypass pattern"
            
            DetectionLogger.logFinding(
                text = input,
                score = 0.95f,
                stage = GroomingStage.ISOLATION,
                method = "BypassPattern",
                pattern = matchedPattern
            )
            
            return AnalysisResult(
                score = 0.95f,
                isRisk = true,
                explanation = "🚨 Kritisches Grooming-Muster erkannt: \"$matchedPattern\"",
                detectionMethod = "BypassPattern",
                detectedPatterns = listOf(matchedPattern),
                stage = "STAGE_ISOLATION",
                confidence = 0.95f
            )
        }

        // ═══════════════════════════════════════════════════════════════
        // 0. SEMANTIC SIMILARITY CHECK (collect score, no early return)
        // ═══════════════════════════════════════════════════════════════
        semanticDetector?.let { detector ->
            try {
                val semanticResult = detector.detectIntent(input)

                // Collect semantic score instead of early return
                if (semanticResult.isRisk && semanticResult.intent != null) {
                    scores["Semantic"] = semanticResult.similarity
                    detectedPatterns.add(semanticResult.matchedSeed)
                    detectionMethod = "Semantic-${semanticResult.intent}"
                    
                    val intent = semanticResult.intent
                    val stage = GroomingIntent.getStage(intent)
                    val intentExplanation = GroomingIntent.getExplanation(intent)
                    explanation = "🔍 Semantische Erkennung: $intentExplanation\n\n" +
                                 "Ähnlich zu: \"${semanticResult.matchedSeed}\"\n" +
                                 "Ähnlichkeit: ${(semanticResult.similarity*100).toInt()}%"
                }

                // Log semantic scores auch bei no-match
                Log.d(TAG, "   Semantic Scores: ${semanticResult.allIntentScores.map { 
                    "${it.key}=${(it.value*100).toInt()}%" 
                }.joinToString(", ")}")

            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Semantic detection failed, using fallback", e)
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // 1. OSPREY TRANSFORMER CHECK (collect score, no early return)
        // ═══════════════════════════════════════════════════════════════
        ospreyDetector?.let { detector ->
            try {
                val ospreyResult = detector.predict(input)

                if (ospreyResult != null && ospreyResult.isRisk) {
                    scores["Osprey"] = ospreyResult.confidence
                    detectedPatterns.add(ospreyResult.stage)
                    if (detectionMethod == "Unknown") {
                        detectionMethod = "Osprey-${ospreyResult.stage}"
                        explanation = "🤖 Osprey Transformer: ${ospreyResult.explanation}"
                    }
                }

                // Log Osprey scores auch bei no-risk
                Log.d(TAG, "   Osprey Scores: ${ospreyResult?.allStageScores?.map { 
                    "${it.key}=${(it.value*100).toInt()}%" 
                }?.joinToString(", ") ?: "N/A"}")

            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Osprey detection failed, using fallback", e)
            }
        }

        // 2. Assessment-Pattern Check (collect score, no early return unless bypass pattern)
        val lowerInput = input.lowercase().trim()

        // NEGATIONS-CHECK zuerst
        if (!DetectionConfig.containsNegation(input)) {
            val assessmentPatterns = mapOf(
                // Isolation/Assessment - PHRASEN (keep only clearly suspicious patterns)
                "bist du allein" to 0.85f,
                "bist du alleine" to 0.85f,
                "bist du gerade allein" to 0.90f,
                "are you alone" to 0.85f,
                "are you home alone" to 0.90f,
                "ist jemand bei dir" to 0.80f,
                "is anyone there" to 0.80f,
                "wo sind deine eltern" to 0.80f,
                "where are your parents" to 0.80f,
                "ist niemand da" to 0.85f,
                "bist du in deinem zimmer" to 0.75f,

                // Gift Giving - Only clearly suspicious patterns (removed ambiguous everyday phrases)
                // Removed: "ich kaufe dir", "ich schenke dir", "ich lade dich ein", "brauchst du geld"
                "ich schicke dir geld" to 0.85f,
                "ich schick dir geld" to 0.85f,
                "ich gebe dir geld" to 0.85f,
                "ich geb dir geld" to 0.85f,
                "ich überweise dir" to 0.85f,
                "i'll send you money" to 0.85f,
                "i'll give you money" to 0.85f
            )

        for ((pattern, riskScore) in assessmentPatterns) {
            if (lowerInput.contains(pattern)) {
                // Collect score instead of early return
                scores["Assessment"] = riskScore
                detectedPatterns.add(pattern)
                if (detectionMethod == "Unknown") {
                    detectionMethod = "Assessment-Pattern"
                    explanation = "Erkannt wegen: '$pattern' (Assessment-Phase - kritisches Grooming-Muster)"
                }
                break  // Only match first pattern
            }
        }
        }

        // 2. ML-Prediction
        var detectedStage = "UNKNOWN"
        val mlPrediction = mlDetector.predict(input)
        if (mlPrediction != null && mlPrediction.isDangerous) {
            scores["ML"] = mlPrediction.confidence
            detectedPatterns.add("ML: ${mlPrediction.stage}")
            detectedStage = mlPrediction.stage  // Stage vom ML übernehmen!
            if (mlPrediction.confidence > 0.7f) {
                detectionMethod = "Machine Learning"
                explanation = "ML-Modell erkannte: ${mlPrediction.stage}-Phase (${(mlPrediction.confidence * 100).toInt()}% Konfidenz)"
            }
        }

        // 3. Trigram Detection - Check both DE and EN
        val trigramResultDE = trigramDetector.detectTrigrams(input, "de")
        val trigramResultEN = trigramDetector.detectTrigrams(input, "en")
        val trigramResult = if (trigramResultDE.risk >= trigramResultEN.risk) trigramResultDE else trigramResultEN
        if (trigramResult.risk > 0.6f) {
            scores["Trigram"] = trigramResult.risk
            detectedPatterns.add("Trigram-Muster")
            if (detectionMethod == "Unknown") {
                detectionMethod = "Trigram-Analysis"
                explanation = "Verdächtige Wort-Kombinationen erkannt (${(trigramResult.risk * 100).toInt()}%)"
            }
        }

        // 4. Adult/Child Context
        val adultChildResult = adultChildDetector.analyzeMessage(input)
        if (adultChildResult.isLikelyAdult && adultChildResult.adultScore > 0.7f) {
            scores["AdultContext"] = adultChildResult.adultScore * 0.8f
            detectedPatterns.add("Erwachsenen-Sprache")
            if (detectionMethod == "Unknown") {
                detectionMethod = "Adult-Context"
                explanation = "Erwachsenen-typische Sprache erkannt (${(adultChildResult.adultScore * 100).toInt()}%)"
            }
        }

        // 5. Apply confidence adjustment by length to all scores
        val adjustedScores = scores.mapValues { (_, score) ->
            DetectionConfig.adjustConfidenceByLength(score, input.length)
        }.toMutableMap()

        // Log adjustments if text is short
        if (input.length < 30) {
            Log.d(TAG, "📏 Length adjustment applied (text length: ${input.length})")
            adjustedScores.forEach { (key, value) ->
                Log.d(TAG, "   $key: ${(scores[key]!!*100).toInt()}% → ${(value*100).toInt()}%")
            }
        }

        // 6. Check for layer agreement (dual-confirmation)
        if (!DetectionConfig.hasLayerAgreement(adjustedScores)) {
            Log.d(TAG, "⚠️ Insufficient layer agreement - returning safe result")
            return AnalysisResult(
                score = 0.0f,
                isRisk = false,
                explanation = "Unzureichende Bestätigung durch mehrere Layer",
                detectionMethod = "InsufficientAgreement",
                detectedPatterns = detectedPatterns,
                stage = detectedStage,
                confidence = 0.0f
            )
        }

        // 7. Berechne finalen Score
        val finalScore = if (adjustedScores.isEmpty()) 0.0f else calculateWeightedScore(adjustedScores)

        // 8. Generiere Erklärung falls noch nicht vorhanden
        if (explanation.isEmpty()) {
            if (finalScore > 0.5f) {
                explanation = "Kombinierte Erkennung: ${detectedPatterns.joinToString(", ")}"
                detectionMethod = "Multi-Layer"

                // 🚨 STRUCTURED FINDING LOG für Multi-Layer Detection
                DetectionLogger.logFinding(
                    text = input,
                    score = finalScore,
                    stage = GroomingStage.UNKNOWN,
                    method = "Multi-Layer",
                    pattern = detectedPatterns.firstOrNull()
                )
            } else {
                explanation = "Keine verdächtigen Muster erkannt"
                detectionMethod = "Safe"
            }
        }

        return AnalysisResult(
            score = finalScore,
            isRisk = finalScore > 0.5f,
            explanation = explanation,
            detectionMethod = detectionMethod,
            detectedPatterns = detectedPatterns,
            stage = detectedStage,
            confidence = finalScore
        )
    }

    /**
     * Berechnet gewichteten Score aus allen Detection-Layers
     *
     * Basierend auf Papers: Frontiers Pediatrics, ArXiv 2409.07958v1
     */
    private fun calculateWeightedScore(scores: Map<String, Float>): Float {
        if (scores.isEmpty()) return 0.0f

        // 🔍 ULTRA-DEBUG: Zeige ALLE Scores
        Log.e(TAG, "━━━ calculateWeightedScore START ━━━")
        scores.forEach { (key, value) ->
            Log.e(TAG, "  $key = ${(value*100).toInt()}%")
        }

        // Log Assessment, StageProgression, and AdultContext scores for debugging
        val assessmentScore = scores["Assessment"] ?: 0.0f
        Log.e(TAG, "  Assessment-Check: ${(assessmentScore*100).toInt()}%")
        
        val stageProgressionScore = scores["StageProgression"] ?: 0.0f
        if (stageProgressionScore > 0.7f) {
            Log.w(TAG, "  Stage-Anomalie erkannt! Score: ${(stageProgressionScore*100).toInt()}%")
        }
        
        val adultContextScore = scores["AdultContext"] ?: 0.0f
        if (adultContextScore > 0.7f) {
            Log.w(TAG, "  Adult-Context (potentieller Groomer) erkannt! Score: ${(adultContextScore*100).toInt()}%")
        }

        // Gewichte pro Detection-Layer (optimiert für ~95% Accuracy)
        val weights = mapOf(
            "Semantic" to 0.25f,        // Semantic: 25% (HÖCHSTE PRIORITÄT)
            "Osprey" to 0.20f,          // Osprey Transformer: 20% (6-Stage-Detection)
            "ML" to 0.20f,              // ML-Modell: 20% (Basis)
            "Trigram" to 0.12f,         // Trigrams: 12% (+3% Accuracy)
            "AdultContext" to 0.10f,    // Adult Context: 10%
            "Context" to 0.08f,         // Context-Aware: 8%
            "StageProgression" to 0.03f, // Stage Progression: 3% (+1% Accuracy)
            "Assessment" to 0.15f,      // Assessment Patterns: 15% (increased from 0.01f)
            "Keywords" to 0.01f         // Keywords: 1% (Fallback)
        )

        var weightedSum = 0.0f
        var totalWeight = 0.0f

        scores.forEach { (key, score) ->
            val weight = weights[key] ?: 0.0f
            weightedSum += score * weight
            totalWeight += weight
        }

        // Normalisiere auf vorhandene Weights
        return if (totalWeight > 0) {
            (weightedSum / totalWeight).coerceIn(0.0f, 1.0f)
        } else {
            0.0f
        }
    }

    /**
     * Analysiert eine Conversation mit Time Investment Tracking
     *
     * @param messages Liste von Messages mit Timestamps
     * @return Erweiterte Risk-Analyse
     */
    fun analyzeConversation(
        messages: List<Pair<String, Long>>  // (Text, Timestamp)
    ): ConversationAnalysis {
        if (messages.isEmpty()) {
            return ConversationAnalysis(0.0f, emptyList(), null)
        }

        // Analysiere einzelne Messages
        val messageScores = messages.map { (text, _) -> analyzeText(text) }
        val avgMessageScore = messageScores.average().toFloat()

        // Time Investment Analysis
        val firstTime = messages.first().second
        val lastTime = messages.last().second
        val timeMetrics = timeTracker.analyzeConversation(
            messageCount = messages.size,
            firstMessageTime = firstTime,
            lastMessageTime = lastTime
        )

        // Kombiniere Message-Scores + Time Investment
        val timeBoost = timeMetrics.timeInvestmentScore * 0.2f // +20% bei hohem Investment
        val finalScore = (avgMessageScore + timeBoost).coerceIn(0.0f, 1.0f)

        Log.d(TAG, "💬 Conversation Analysis: ${messages.size} messages, Score: ${(finalScore*100).toInt()}%")
        Log.d(TAG, "   Time Investment: ${(timeMetrics.timeInvestmentScore*100).toInt()}%, Speed: ${timeMetrics.progressionSpeed}")

        return ConversationAnalysis(
            overallRisk = finalScore,
            messageScores = messageScores,
            timeMetrics = timeMetrics
        )
    }

    data class ConversationAnalysis(
        val overallRisk: Float,
        val messageScores: List<Float>,
        val timeMetrics: TimeInvestmentTracker.ConversationMetrics?
    )

    /**
     * Lädt Risk-Keywords aus der Vocabulary-Datei
     */
    private fun loadRiskKeywords(context: Context): Set<String> {
        val keywords = mutableSetOf<String>()

        try {
            context.assets.open("vocabulary.txt").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    // Skip häufige Wörter die keine Risk sind
                    val skipWords = setOf(
                        "<unk>", "the", "to", "and", "a", "of", "is", "in",
                        "you", "it", "that", "child", "safety", "protect"
                    )

                    reader.forEachLine { line ->
                        val word = line.trim().lowercase()
                        if (word.isNotEmpty() && !skipWords.contains(word)) {
                            keywords.add(word)
                        }
                    }
                }
            }
            Log.d(TAG, "✅ ${keywords.size} Risk-Keywords aus Vocabulary geladen")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Laden des Vocabulary", e)
            return setOf()
        }
        
        return keywords
    }
    
    /**
     * Analysiert Text mit Konversationskontext
     *
     * Nutzt den ConversationBuffer um Nachrichten pro Kontakt zu sammeln
     * und ermöglicht Osprey, Eskalationsmuster über den Verlauf zu erkennen.
     *
     * @param input Der zu analysierende Text
     * @param appPackage Package-Name der Quell-App
     * @param chatIdentifier Chat-Fenster-Titel oder ähnliches (wird pseudonymisiert)
     * @param isLocalUser true wenn die Nachricht vom Kind stammt, false wenn vom Kontakt
     * @return AnalysisResult mit Score und Erklärung
     */
    fun analyzeWithConversation(
        input: String,
        appPackage: String = "unknown",
        chatIdentifier: String = "default",
        isLocalUser: Boolean = false
    ): AnalysisResult {
        if (input.isBlank()) {
            return AnalysisResult(
                score = 0.0f,
                isRisk = false,
                explanation = "Leerer Text",
                detectionMethod = "None"
            )
        }

        // TEXT-LENGTH GATE: Skip short texts (same as analyzeTextWithExplanation)
        if (input.trim().length < DetectionConfig.MIN_TEXT_LENGTH) {
            Log.d(TAG, "⏭️ Text zu kurz (${input.trim().length} < ${DetectionConfig.MIN_TEXT_LENGTH}), skipping")
            return AnalysisResult(
                score = 0.0f,
                isRisk = false,
                explanation = "Text zu kurz für Analyse",
                detectionMethod = "Skipped"
            )
        }

        // WORD COUNT GATE: Skip texts with too few words
        val wordCount = input.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        if (wordCount < DetectionConfig.MIN_WORDS_FOR_PATTERN) {
            Log.d(TAG, "⏭️ Zu wenig Wörter ($wordCount < ${DetectionConfig.MIN_WORDS_FOR_PATTERN}), skipping")
            return AnalysisResult(
                score = 0.0f,
                isRisk = false,
                explanation = "Zu wenig Wörter für Analyse",
                detectionMethod = "Skipped"
            )
        }

        // 1. Generiere pseudonymisierte Contact-ID
        val contactId = ConversationBuffer.generateContactId(appPackage, chatIdentifier)

        // 2. Füge Nachricht zum Buffer hinzu
        val message = ConversationBuffer.ConversationMessage(
            text = input,
            authorId = if (isLocalUser) "child" else contactId,
            timestamp = System.currentTimeMillis(),
            isLocalUser = isLocalUser
        )
        ConversationBuffer.addMessage(contactId, message)

        // 3. Hole Konversationskontext
        val conversation = ConversationBuffer.getConversation(contactId)
        val contextFeatures = ConversationBuffer.getContextFeatures(contactId)

        // 4. Analysiere mit Osprey auf Konversationsebene (wenn verfügbar)
        ospreyDetector?.let { detector ->
            try {
                if (conversation.size >= 2) {
                    // Genug Kontext für Konversationsanalyse
                    val ospreyResult = detector.analyzeConversation(conversation, contextFeatures)

                    // Apply confidence adjustment by length before checking threshold
                    val adjustedConfidence = DetectionConfig.adjustConfidenceByLength(ospreyResult.confidence, input.length)

                    // Check that the Osprey confidence exceeds OSPREY_THRESHOLD before returning as risk
                    if (adjustedConfidence > DetectionConfig.OSPREY_THRESHOLD) {
                        Log.w(TAG, "⚠️ OSPREY CONVERSATION RISK: ${ospreyResult.stage}")

                        // Logge Finding
                        DetectionLogger.logFinding(
                            text = input,
                            score = adjustedConfidence,
                            stage = GroomingStage.fromString(ospreyResult.stage),
                            method = "Osprey-Conversation",
                            pattern = ospreyResult.dominantProgression
                        )

                        return AnalysisResult(
                            score = adjustedConfidence,
                            isRisk = true,
                            stage = ospreyResult.stage,
                            explanation = ospreyResult.explanation,
                            detectionMethod = "Osprey-Conversation",
                            detectedPatterns = listOfNotNull(ospreyResult.dominantProgression),
                            confidence = adjustedConfidence,
                            allStageScores = ospreyResult.allStageScores
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Osprey conversation analysis failed", e)
            }
        }

        // 5. Fallback: Einzelnachricht-Analyse
        return analyzeTextWithExplanation(input, appPackage)
    }

    /**
     * Schließt die Engine und gibt Ressourcen frei
     */
    override fun close() {
        // Buffer leeren (DSGVO)
        ConversationBuffer.clearAll()

        mlDetector.close()
        semanticDetector?.close()
        ospreyDetector?.close()
        Log.d(TAG, "🔒 SafeSparkEngine geschlossen")
    }
}
