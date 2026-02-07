package com.example.safespark

import android.content.Context
import android.util.Log
import com.example.safespark.ml.OspreyLocalDetector
import com.example.safespark.ml.ConversationBuffer
import com.example.safespark.detection.SemanticDetector
import com.example.safespark.model.GroomingIntent
import com.example.safespark.logging.DetectionLogger
import com.example.safespark.logging.DetectionLogger.GroomingStage
import com.example.safespark.config.DetectionConfig
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
 * Simplified Architecture with 3 Components:
 * 1. Osprey (Primary) - BERT/RoBERTa-based 6-stage grooming detection
 * 2. SemanticDetector (Fallback) - MiniLM-based similarity detection
 * 3. Bypass Patterns (Always Active) - 8 high-confidence phrases
 */
class KidGuardEngine(private val context: Context) : Closeable {

    private val semanticDetector: SemanticDetector?  // Fallback detector
    private val ospreyDetector: OspreyLocalDetector?  // Primary detector
    private val TAG = "SafeSparkEngine"

    init {
        try {
            // Initialize Osprey Detector (Primary)
            ospreyDetector = try {
                OspreyLocalDetector(context).also {
                    Log.d(TAG, "✅ Osprey Transformer-Detector initialisiert (PRIMARY - 6 Stages)")
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Osprey Detector nicht verfügbar (Modell fehlt)", e)
                null
            }

            // Initialize Semantic Detector (Fallback)
            semanticDetector = try {
                SemanticDetector(context).also {
                    Log.d(TAG, "✅ Semantic Detector initialisiert (FALLBACK)")
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Semantic Detector konnte nicht geladen werden", e)
                null
            }

            val detectorStatus = when {
                ospreyDetector != null && semanticDetector != null -> "PRIMARY + FALLBACK verfügbar"
                ospreyDetector != null -> "NUR PRIMARY verfügbar"
                semanticDetector != null -> "NUR FALLBACK verfügbar"
                else -> "KEINE Detektoren verfügbar"
            }
            Log.d(TAG, "🎯 Engine initialisiert: $detectorStatus")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Initialisieren der Engine", e)
            throw RuntimeException("Fehler beim Initialisieren des SafeSparkEngine", e)
        }
    }
    
    /**
     * Analysiert einen Text und gibt einen Risk-Score (0.0-1.0) zurück
     * 
     * @deprecated Use analyzeTextWithExplanation() or analyzeWithConversation() instead
     * @param input Der zu analysierende Text
     * @param appPackage Die App, aus der der Text stammt
     * @return Risk-Score (0.0 = safe, 1.0 = high risk)
     */
    @Deprecated("Use analyzeTextWithExplanation() or analyzeWithConversation() instead", 
        ReplaceWith("analyzeTextWithExplanation(input, appPackage).score"))
    fun analyzeText(input: String, appPackage: String = "unknown"): Float {
        return analyzeTextWithExplanation(input, appPackage).score
    }

    /**
     * Analysiert Text mit Erklärung (Explainable AI)
     *
     * Architecture:
     * 1. Text gates (MIN_TEXT_LENGTH, MIN_WORDS_FOR_PATTERN)
     * 2. Bypass pattern check → immediate 0.95f
     * 3. Try Osprey predict() → use Osprey score if available
     * 4. Fallback: Try SemanticDetector → use semantic score if available
     * 5. Neither available → return 0.0f (safe, log warning)
     *
     * @param input Der zu analysierende Text
     * @param appPackage Package-Name der Quell-App
     * @return AnalysisResult mit Score und Erklärung
     */
    fun analyzeTextWithExplanation(
        input: String, 
        appPackage: String = "unknown"
    ): AnalysisResult {
        // 1. Empty/blank check
        if (input.isBlank()) {
            return AnalysisResult(
                score = 0.0f,
                isRisk = false,
                explanation = "Leerer Text",
                detectionMethod = "None"
            )
        }

        // 2. TEXT-LENGTH GATE: Skip short texts
        if (input.trim().length < DetectionConfig.MIN_TEXT_LENGTH) {
            Log.d(TAG, "⏭️ Text zu kurz (${input.trim().length} < ${DetectionConfig.MIN_TEXT_LENGTH}), safe result")
            return AnalysisResult(
                score = 0.0f,
                isRisk = false,
                explanation = "Text zu kurz für Analyse",
                detectionMethod = "TextLengthGate"
            )
        }

        // 3. WORD COUNT GATE: Skip texts with too few words
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

        // 4. CHECK FOR BYPASS PATTERNS (these trigger immediately!)
        if (DetectionConfig.matchesBypassPattern(input)) {
            Log.w(TAG, "🚨 BYPASS PATTERN detected - immediate alert!")
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

        // 5. Try Osprey predict() (PRIMARY)
        ospreyDetector?.let { detector ->
            try {
                val ospreyResult = detector.predict(input)

                if (ospreyResult != null) {
                    if (ospreyResult.isRisk) {
                        Log.w(TAG, "⚠️ OSPREY RISK: ${ospreyResult.stage} (${(ospreyResult.confidence*100).toInt()}%)")
                        
                        DetectionLogger.logFinding(
                            text = input,
                            score = ospreyResult.confidence,
                            stage = GroomingStage.fromString(ospreyResult.stage),
                            method = "Osprey",
                            pattern = ospreyResult.stage
                        )
                        
                        return AnalysisResult(
                            score = ospreyResult.confidence,
                            isRisk = true,
                            explanation = "🤖 Osprey Transformer: ${ospreyResult.explanation}",
                            detectionMethod = "Osprey-${ospreyResult.stage}",
                            detectedPatterns = listOf(ospreyResult.stage),
                            stage = ospreyResult.stage,
                            confidence = ospreyResult.confidence,
                            allStageScores = ospreyResult.allStageScores
                        )
                    } else {
                        // Osprey says safe
                        Log.d(TAG, "✅ Osprey: Safe (highest stage score: ${(ospreyResult.allStageScores.values.maxOrNull() ?: 0f) * 100}%)")
                        return AnalysisResult(
                            score = 0.0f,
                            isRisk = false,
                            explanation = "Osprey: Keine verdächtigen Muster erkannt",
                            detectionMethod = "Osprey-Safe",
                            stage = "SAFE",
                            confidence = 0.0f,
                            allStageScores = ospreyResult.allStageScores
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Osprey detection failed, trying fallback", e)
            }
        }

        // 6. Fallback: Try SemanticDetector
        semanticDetector?.let { detector ->
            try {
                val semanticResult = detector.detectIntent(input)

                if (semanticResult.isRisk && semanticResult.intent != null) {
                    Log.w(TAG, "⚠️ SEMANTIC RISK: ${semanticResult.intent} (${(semanticResult.similarity*100).toInt()}%)")
                    
                    val intent = semanticResult.intent
                    val stage = GroomingIntent.getStage(intent)
                    val intentExplanation = GroomingIntent.getExplanation(intent)
                    
                    DetectionLogger.logFinding(
                        text = input,
                        score = semanticResult.similarity,
                        stage = GroomingStage.fromString(stage),
                        method = "Semantic",
                        pattern = semanticResult.matchedSeed ?: intent
                    )
                    
                    return AnalysisResult(
                        score = semanticResult.similarity,
                        isRisk = true,
                        explanation = "🔍 Semantische Erkennung: $intentExplanation\n\n" +
                                     "Ähnlich zu: \"${semanticResult.matchedSeed}\"\n" +
                                     "Ähnlichkeit: ${(semanticResult.similarity*100).toInt()}%",
                        detectionMethod = "Semantic-${semanticResult.intent}",
                        detectedPatterns = listOfNotNull(semanticResult.matchedSeed),
                        stage = stage,
                        confidence = semanticResult.similarity
                    )
                } else {
                    // Semantic says safe
                    Log.d(TAG, "✅ Semantic: Safe (max similarity: ${(semanticResult.allIntentScores.values.maxOrNull() ?: 0f) * 100}%)")
                    return AnalysisResult(
                        score = 0.0f,
                        isRisk = false,
                        explanation = "Semantic: Keine verdächtigen Muster erkannt",
                        detectionMethod = "Semantic-Safe",
                        stage = "SAFE",
                        confidence = 0.0f
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Semantic detection failed", e)
            }
        }

        // 7. Neither detector available
        Log.w(TAG, "⚠️ Keine Detektoren verfügbar - returning safe result")
        return AnalysisResult(
            score = 0.0f,
            isRisk = false,
            explanation = "Keine Detektoren verfügbar für Analyse",
            detectionMethod = "NoDetectorsAvailable",
            stage = "SAFE",
            confidence = 0.0f
        )
    }

    /**
     * Analysiert Text mit Konversationskontext
     *
     * Architecture:
     * 1. Text gates
     * 2. Add message to ConversationBuffer
     * 3. Try Osprey analyzeConversation() with conversation buffer messages
     *    - If risk detected: return result
     * 4. Fallback to single-message analysis via analyzeTextWithExplanation()
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
        // 1. Empty/blank check
        if (input.isBlank()) {
            return AnalysisResult(
                score = 0.0f,
                isRisk = false,
                explanation = "Leerer Text",
                detectionMethod = "None"
            )
        }

        // 2. TEXT-LENGTH GATE
        if (input.trim().length < DetectionConfig.MIN_TEXT_LENGTH) {
            Log.d(TAG, "⏭️ Text zu kurz (${input.trim().length} < ${DetectionConfig.MIN_TEXT_LENGTH}), skipping")
            return AnalysisResult(
                score = 0.0f,
                isRisk = false,
                explanation = "Text zu kurz für Analyse",
                detectionMethod = "TextLengthGate"
            )
        }

        // 3. WORD COUNT GATE
        val wordCount = input.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        if (wordCount < DetectionConfig.MIN_WORDS_FOR_PATTERN) {
            Log.d(TAG, "⏭️ Zu wenig Wörter ($wordCount < ${DetectionConfig.MIN_WORDS_FOR_PATTERN}), skipping")
            return AnalysisResult(
                score = 0.0f,
                isRisk = false,
                explanation = "Zu wenig Wörter für Analyse",
                detectionMethod = "WordCountGate"
            )
        }

        // 4. Add message to ConversationBuffer
        val contactId = ConversationBuffer.generateContactId(appPackage, chatIdentifier)
        val message = ConversationBuffer.ConversationMessage(
            text = input,
            authorId = if (isLocalUser) "child" else contactId,
            timestamp = System.currentTimeMillis(),
            isLocalUser = isLocalUser
        )
        ConversationBuffer.addMessage(contactId, message)

        // 5. Get conversation context
        val conversation = ConversationBuffer.getConversation(contactId)
        val contextFeatures = ConversationBuffer.getContextFeatures(contactId)

        // 6. Try Osprey conversation analysis (if available and enough context)
        ospreyDetector?.let { detector ->
            try {
                if (conversation.size >= 2) {
                    val ospreyResult = detector.analyzeConversation(conversation, contextFeatures)

                    if (ospreyResult.isRisk && ospreyResult.confidence > DetectionConfig.OSPREY_THRESHOLD) {
                        Log.w(TAG, "⚠️ OSPREY CONVERSATION RISK: ${ospreyResult.stage} (${(ospreyResult.confidence*100).toInt()}%)")
                        
                        DetectionLogger.logFinding(
                            text = input,
                            score = ospreyResult.confidence,
                            stage = GroomingStage.fromString(ospreyResult.stage),
                            method = "Osprey-Conversation",
                            pattern = ospreyResult.dominantProgression
                        )
                        
                        return AnalysisResult(
                            score = ospreyResult.confidence,
                            isRisk = true,
                            explanation = "🔍 Osprey Conversation Analysis: ${ospreyResult.explanation}\n\n" +
                                         "Dominante Progression: ${ospreyResult.dominantProgression}",
                            detectionMethod = "Osprey-Conversation",
                            detectedPatterns = listOf(ospreyResult.dominantProgression),
                            stage = ospreyResult.stage,
                            confidence = ospreyResult.confidence,
                            allStageScores = ospreyResult.allStageScores
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Osprey conversation analysis failed, using fallback", e)
            }
        }

        // 7. Fallback to single-message analysis
        return analyzeTextWithExplanation(input, appPackage)
    }

    /**
     * Schließt die Engine und gibt Ressourcen frei
     */
    override fun close() {
        // Buffer leeren (DSGVO)
        ConversationBuffer.clearAll()

        semanticDetector?.close()
        ospreyDetector?.close()
        Log.d(TAG, "🔒 SafeSparkEngine geschlossen")
    }
}
