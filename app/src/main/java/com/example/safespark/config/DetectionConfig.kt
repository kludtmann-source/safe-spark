package com.example.safespark.config

/**
 * Zentrale Konfiguration für Grooming-Detection
 *
 * Alle Thresholds und Einstellungen an einem Ort.
 * Optimiert basierend auf False-Positive-Analyse.
 */
object DetectionConfig {

    // ═══════════════════════════════════════════════════════════════
    // THRESHOLDS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Semantic Similarity Threshold (MiniLM Encoder)
     *
     * Vorher: 0.75f → zu viele False Positives bei ähnlichen Phrasen
     * Jetzt: 0.82f → bessere Separation
     */
    const val SEMANTIC_THRESHOLD = 0.82f

    /**
     * ML Grooming Model Threshold (TFLite CNN)
     *
     * Vorher: 0.3f (!) → viel zu niedrig
     * Jetzt: 0.65f → realistischer
     */
    const val ML_THRESHOLD = 0.65f

    /**
     * Pattern Matching Threshold
     *
     * Minimum Score für Assessment-Patterns
     */
    const val PATTERN_THRESHOLD = 0.80f

    /**
     * Osprey Transformer Default Threshold
     */
    const val OSPREY_THRESHOLD = 0.70f

    /**
     * Trigram Detection Threshold
     */
    const val TRIGRAM_THRESHOLD = 0.60f

    // ═══════════════════════════════════════════════════════════════
    // TEXT REQUIREMENTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Minimum text length for analysis
     *
     * Kurze Texte wie "ok", "ja" → skip
     */
    const val MIN_TEXT_LENGTH = 10

    /**
     * Minimum words required for pattern matching
     *
     * Einzelwörter → kein Pattern-Match möglich
     */
    const val MIN_WORDS_FOR_PATTERN = 3

    // ═══════════════════════════════════════════════════════════════
    // CONFIRMATION REQUIREMENTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Require multiple layers to confirm detection
     *
     * true → mindestens 2 Layer müssen übereinstimmen
     * false → ein Layer reicht für Alert
     */
    const val REQUIRE_DUAL_CONFIRMATION = true

    /**
     * Minimum number of layers that must agree for alert
     *
     * Nur relevant wenn REQUIRE_DUAL_CONFIRMATION = true
     */
    const val MIN_LAYERS_FOR_ALERT = 2

    /**
     * Confidence threshold for layer to count as "agreeing"
     */
    const val LAYER_AGREEMENT_THRESHOLD = 0.60f

    // ═══════════════════════════════════════════════════════════════
    // SPECIAL PATTERNS (bypass dual confirmation)
    // ═══════════════════════════════════════════════════════════════

    /**
     * High-confidence patterns that bypass dual confirmation
     *
     * Diese Phrasen sind SO eindeutig, dass ein Match reicht.
     */
    val BYPASS_PATTERNS = listOf(
        "schick mir ein foto",
        "send me a pic",
        "zeig dich mir",
        "show yourself",
        "erzähl niemandem davon",
        "don't tell anyone",
        "unser geheimnis",
        "our secret"
    )

    // ═══════════════════════════════════════════════════════════════
    // NEGATION PATTERNS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Patterns that negate risk (reduce score)
     *
     * "Ich bin NICHT allein" → kein Risk
     */
    val NEGATION_PATTERNS = listOf(
        "nicht allein",
        "not alone",
        "bin nicht allein",
        "i'm not alone",
        "eltern sind da",
        "parents are here",
        "mama ist da",
        "papa ist da"
    )

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Check if text contains a negation pattern
     */
    fun containsNegation(text: String): Boolean {
        val lowerText = text.lowercase()
        return NEGATION_PATTERNS.any { lowerText.contains(it) }
    }

    /**
     * Check if text matches a bypass pattern
     */
    fun matchesBypassPattern(text: String): Boolean {
        val lowerText = text.lowercase()
        return BYPASS_PATTERNS.any { lowerText.contains(it) }
    }

    /**
     * Adjust confidence based on text length
     *
     * Kurze Texte → reduzierte Confidence
     */
    fun adjustConfidenceByLength(score: Float, textLength: Int): Float {
        return when {
            textLength < 15 -> score * 0.6f   // Stark reduzieren
            textLength < 30 -> score * 0.8f   // Leicht reduzieren
            else -> score                      // Volle Confidence
        }
    }

    /**
     * Check if enough layers agree for alert
     */
    fun hasLayerAgreement(scores: Map<String, Float>): Boolean {
        if (!REQUIRE_DUAL_CONFIRMATION) return true

        val agreeingLayers = scores.count { it.value >= LAYER_AGREEMENT_THRESHOLD }
        return agreeingLayers >= MIN_LAYERS_FOR_ALERT
    }
}
