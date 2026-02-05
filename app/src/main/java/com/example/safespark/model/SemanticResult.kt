package com.example.safespark.model

import com.example.safespark.AnalysisResult

/**
 * Result from semantic similarity detection
 *
 * Contains the detected intent (if any), similarity score,
 * and which seed pattern matched best.
 */
data class SemanticResult(
    /**
     * Detected intent type or null if no match above threshold
     *
     * Possible values:
     * - "SUPERVISION_CHECK"
     * - "SECRECY_REQUEST"
     * - "PHOTO_REQUEST"
     * - "MEETING_REQUEST"
     * - null (no match)
     */
    val intent: String? = null,

    /**
     * Highest cosine similarity score (0.0 - 1.0)
     */
    val similarity: Float = 0f,

    /**
     * The seed pattern that matched best
     * Example: "Bist du alleine?"
     */
    val matchedSeed: String? = null,

    /**
     * All intent scores for debugging
     * Maps intent name → max similarity score
     */
    val allIntentScores: Map<String, Float> = emptyMap(),

    /**
     * Detection method for logging
     */
    val detectionMethod: String = "Semantic",

    /**
     * Whether this result triggered a risk alert
     */
    val isRisk: Boolean = false
) {
    /**
     * Convert semantic result to AnalysisResult for engine integration
     */
    fun toAnalysisResult(
        stage: String = "ASSESSMENT",
        explanation: String = ""
    ): AnalysisResult {
        return AnalysisResult(
            score = similarity,
            isRisk = isRisk,
            stage = stage,
            explanation = explanation,
            detectionMethod = "Semantic-$intent",
            detectedPatterns = listOfNotNull(matchedSeed),
            confidence = similarity,
            allStageScores = mapOf(stage to similarity)
        )
    }

    companion object {
        /**
         * Create a "no match" result
         */
        fun noMatch(maxScore: Float = 0f): SemanticResult {
            return SemanticResult(
                intent = null,
                similarity = maxScore,
                matchedSeed = null,
                isRisk = false
            )
        }
    }
}

/**
 * Intent categories for grooming detection
 */
enum class GroomingIntent(
    val intentName: String,
    val threshold: Float,
    val stage: String,
    val explanation: String
) {
    SUPERVISION_CHECK(
        intentName = "SUPERVISION_CHECK",
        threshold = 0.80f,  // erhöht von 0.75f
        stage = "ASSESSMENT",
        explanation = "Täter prüft ob Kind alleine/unbeaufsichtigt ist"
    ),

    SECRECY_REQUEST(
        intentName = "SECRECY_REQUEST",
        threshold = 0.85f,  // erhöht von 0.78f
        stage = "SECRECY_ENFORCEMENT",
        explanation = "Täter fordert Geheimhaltung ('Sag niemandem davon')"
    ),

    PHOTO_REQUEST(
        intentName = "PHOTO_REQUEST",
        threshold = 0.80f,
        stage = "SEXUALIZATION",
        explanation = "Täter fordert Bilder/Fotos"
    ),

    MEETING_REQUEST(
        intentName = "MEETING_REQUEST",
        threshold = 0.75f,
        stage = "PHYSICAL_MEETING",
        explanation = "Täter will sich persönlich treffen"
    );

    companion object {
        fun fromName(name: String): GroomingIntent? {
            return values().find { it.intentName == name }
        }

        fun getThreshold(intent: String): Float {
            return values().find { it.intentName == intent }?.threshold ?: 0.75f
        }

        fun getStage(intent: String): String {
            return values().find { it.intentName == intent }?.stage ?: "ASSESSMENT"
        }

        fun getExplanation(intent: String): String {
            return values().find { it.intentName == intent }?.explanation
                ?: "Semantisch verdächtiges Muster erkannt"
        }
    }
}
