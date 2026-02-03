package com.example.safespark.ml

import android.util.Log

/**
 * Stage Progression Detector
 *
 * Basierend auf Frontiers Pediatrics Paper + Nature Scientific Reports
 * Erkennt Progression durch Grooming-Stages und zeitliche Eskalation
 *
 * Grooming-Stages:
 * 1. TRUST - Vertrauensaufbau
 * 2. NEEDS - Geschenke, Hilfe anbieten
 * 3. ASSESSMENT - "Bist du allein?"
 * 4. ISOLATION - Geheimhaltung, Plattformwechsel
 * 5. SEXUAL - Sexualisierung
 *
 * Performance: +1% Accuracy Improvement
 */
class StageProgressionDetector {

    companion object {
        private const val TAG = "StageProgressionDetector"

        // Normale Progression: 5-10 Tage zwischen Stages
        private const val NORMAL_PROGRESSION_HOURS = 120  // 5 Tage

        // Schnelle Progression: < 2 Tage = kritisch!
        private const val FAST_PROGRESSION_HOURS = 48    // 2 Tage

        // Sehr schnelle Progression: < 12 Stunden = ALARM!
        private const val VERY_FAST_PROGRESSION_HOURS = 12
    }

    data class StageEvent(
        val stage: GroomingStage,
        val timestamp: Long,
        val confidence: Float,
        val messageText: String
    )

    enum class GroomingStage(val order: Int) {
        SAFE(0),
        TRUST(1),
        NEEDS(2),
        ASSESSMENT(3),
        ISOLATION(4),
        SEXUAL(5)
    }

    data class ProgressionAnalysis(
        val stages: List<StageEvent>,
        val progressionSpeed: ProgressionSpeed,
        val riskScore: Float,
        val warnings: List<String>,
        val isAnomalous: Boolean
    )

    enum class ProgressionSpeed {
        VERY_FAST,    // < 12h - KRITISCH!
        FAST,         // < 48h - Erhöht
        NORMAL,       // 5-10 Tage
        SLOW          // > 10 Tage
    }

    /**
     * Analysiert Stage-Progression über Zeit
     */
    fun analyzeProgression(stageHistory: List<StageEvent>): ProgressionAnalysis {
        if (stageHistory.size < 2) {
            return ProgressionAnalysis(
                stages = stageHistory,
                progressionSpeed = ProgressionSpeed.NORMAL,
                riskScore = 0.0f,
                warnings = emptyList(),
                isAnomalous = false
            )
        }

        // Sortiere nach Zeit
        val sortedStages = stageHistory.sortedBy { it.timestamp }

        val warnings = mutableListOf<String>()
        var riskScore = 0.0f

        // 1. Prüfe auf Stage-Skipping (z.B. TRUST → SEXUAL)
        val skippedStages = detectStageSkipping(sortedStages)
        if (skippedStages.isNotEmpty()) {
            riskScore += 0.3f
            warnings.add("⚠️  Stages übersprungen: ${skippedStages.joinToString(", ")}")
            Log.w(TAG, "⚠️  Stage-Skipping detected: $skippedStages")
        }

        // 2. Berechne durchschnittliche Zeit zwischen Stages
        val timeBetweenStages = calculateTimeBetweenStages(sortedStages)
        val avgHours = timeBetweenStages.average()

        // 3. Bestimme Progression Speed
        val speed = when {
            avgHours < VERY_FAST_PROGRESSION_HOURS -> {
                riskScore += 0.5f
                warnings.add("🚨 SEHR SCHNELLE Eskalation: ${avgHours.toInt()}h zwischen Stages!")
                ProgressionSpeed.VERY_FAST
            }
            avgHours < FAST_PROGRESSION_HOURS -> {
                riskScore += 0.3f
                warnings.add("⚠️  Schnelle Eskalation: ${avgHours.toInt()}h zwischen Stages")
                ProgressionSpeed.FAST
            }
            avgHours < NORMAL_PROGRESSION_HOURS -> {
                ProgressionSpeed.NORMAL
            }
            else -> {
                ProgressionSpeed.SLOW
            }
        }

        // 4. Prüfe auf kritische Stage-Kombinationen
        if (hasReachedSexualStage(sortedStages)) {
            riskScore += 0.4f
            warnings.add("🚨 SEXUAL Stage erreicht!")
            Log.e(TAG, "🚨 CRITICAL: Sexual Stage detected!")
        }

        if (hasIsolationPattern(sortedStages)) {
            riskScore += 0.2f
            warnings.add("⚠️  Isolation-Muster erkannt")
        }

        // Normalisiere Risk Score
        riskScore = riskScore.coerceIn(0.0f, 1.0f)

        // Anomalie = sehr schnelle Progression ODER Stage-Skipping + SEXUAL
        val isAnomalous = speed == ProgressionSpeed.VERY_FAST ||
                         (skippedStages.isNotEmpty() && hasReachedSexualStage(sortedStages))

        val analysis = ProgressionAnalysis(
            stages = sortedStages,
            progressionSpeed = speed,
            riskScore = riskScore,
            warnings = warnings,
            isAnomalous = isAnomalous
        )

        if (riskScore > 0.5f) {
            Log.w(TAG, "🎯 Stage Progression Analysis:")
            Log.w(TAG, "   Speed: $speed, Risk: ${(riskScore * 100).toInt()}%")
            Log.w(TAG, "   Stages: ${sortedStages.map { it.stage.name }}")
            warnings.forEach { Log.w(TAG, "   $it") }
        }

        return analysis
    }

    /**
     * Erkennt übersprungene Stages
     */
    private fun detectStageSkipping(stages: List<StageEvent>): List<String> {
        val skipped = mutableListOf<String>()

        for (i in 0 until stages.size - 1) {
            val current = stages[i].stage.order
            val next = stages[i + 1].stage.order

            // Wenn Stage-Order mehr als 1 springt
            if (next - current > 1) {
                val missing = (current + 1 until next).map { order ->
                    GroomingStage.values().first { it.order == order }.name
                }
                skipped.addAll(missing)
            }
        }

        return skipped
    }

    /**
     * Berechnet Zeit zwischen Stages (in Stunden)
     */
    private fun calculateTimeBetweenStages(stages: List<StageEvent>): List<Double> {
        if (stages.size < 2) return emptyList()

        val times = mutableListOf<Double>()
        for (i in 0 until stages.size - 1) {
            val diff = stages[i + 1].timestamp - stages[i].timestamp
            val hours = diff / (1000.0 * 60.0 * 60.0)
            times.add(hours)
        }

        return times
    }

    /**
     * Prüft ob SEXUAL Stage erreicht wurde
     */
    private fun hasReachedSexualStage(stages: List<StageEvent>): Boolean {
        return stages.any { it.stage == GroomingStage.SEXUAL }
    }

    /**
     * Prüft auf Isolation-Muster
     */
    private fun hasIsolationPattern(stages: List<StageEvent>): Boolean {
        return stages.any { it.stage == GroomingStage.ISOLATION }
    }

    /**
     * Konvertiert ML-Stage-String zu GroomingStage Enum
     */
    fun parseStage(stageString: String): GroomingStage {
        return when (stageString.uppercase()) {
            "STAGE_SAFE", "SAFE" -> GroomingStage.SAFE
            "STAGE_TRUST", "TRUST" -> GroomingStage.TRUST
            "STAGE_NEEDS", "NEEDS" -> GroomingStage.NEEDS
            "STAGE_ASSESSMENT", "ASSESSMENT" -> GroomingStage.ASSESSMENT
            "STAGE_ISOLATION", "ISOLATION" -> GroomingStage.ISOLATION
            "STAGE_SEXUAL", "SEXUAL" -> GroomingStage.SEXUAL
            else -> GroomingStage.SAFE
        }
    }

    /**
     * Erstellt StageEvent aus ML-Prediction
     */
    fun createStageEvent(
        stageName: String,
        confidence: Float,
        timestamp: Long,
        messageText: String
    ): StageEvent {
        return StageEvent(
            stage = parseStage(stageName),
            timestamp = timestamp,
            confidence = confidence,
            messageText = messageText
        )
    }
}
