package com.example.safespark.logging

import android.util.Log

/**
 * Dedizierter Logger für positive Grooming-Findings
 *
 * FILTERT alle anderen Logs raus - nur echte Findings werden geloggt.
 *
 * Logcat-Filter: "SafeSpark-ALERT"
 *
 * Jedes Finding enthält:
 * - Input-Text
 * - Score
 * - Grooming-Stage mit Label, Beschreibung und Severity
 * - Detection-Method
 * - Matched Pattern
 */
object DetectionLogger {
    private const val TAG = "SafeSpark-ALERT"

    /**
     * Grooming-Stages nach Osprey/PAN-12 Taxonomie
     *
     * Jede Stage hat:
     * - label: Deutsche Bezeichnung
     * - description: Beispiele/Erklärung
     * - baseSeverity: Basis-Gefährlichkeit (0.0 - 1.0)
     */
    enum class GroomingStage(
        val label: String,
        val description: String,
        val baseSeverity: Float
    ) {
        TRUST_BUILDING(
            label = "Vertrauensaufbau",
            description = "Schmeichelei, 'Du bist so reif', 'Ich verstehe dich'",
            baseSeverity = 0.5f
        ),
        ISOLATION(
            label = "Vom Umfeld isolieren",
            description = "Sag niemandem davon, Plattformwechsel (Telegram, Snapchat)",
            baseSeverity = 0.7f
        ),
        DESENSITIZATION(
            label = "Sexuelles normalisieren",
            description = "Sexuelle Themen beiläufig einführen",
            baseSeverity = 0.8f
        ),
        SEXUAL_CONTENT(
            label = "Explizite Inhalte",
            description = "Bilder fordern, explizite Sprache",
            baseSeverity = 0.9f
        ),
        MAINTENANCE(
            label = "Schweigen erzwingen",
            description = "Das bleibt unter uns, Drohungen",
            baseSeverity = 0.85f
        ),
        ASSESSMENT(
            label = "Situationscheck",
            description = "Bist du alleine?, Wo sind deine Eltern?",
            baseSeverity = 0.6f
        ),
        UNKNOWN(
            label = "Unbekannt",
            description = "Nicht klassifiziert",
            baseSeverity = 0.3f
        );

        companion object {
            /**
             * Map Intent/Stage-String zu GroomingStage enum
             */
            fun fromString(stage: String): GroomingStage {
                return when (stage.uppercase().replace("STAGE_", "")) {
                    "TRUST", "TRUST_BUILDING", "COMPLIMENT" -> TRUST_BUILDING
                    "ISOLATION", "SUPERVISION_CHECK" -> ISOLATION
                    "DESENSITIZATION", "SEXUAL_TOPIC" -> DESENSITIZATION
                    "SEXUAL_CONTENT", "SEXUAL", "PHOTO_REQUEST" -> SEXUAL_CONTENT
                    "MAINTENANCE", "SECRECY", "SECRECY_REQUEST" -> MAINTENANCE
                    "ASSESSMENT" -> ASSESSMENT
                    else -> UNKNOWN
                }
            }
        }
    }

    /**
     * Strukturiertes Finding-Objekt
     */
    data class Finding(
        val inputText: String,
        val score: Float,
        val stage: GroomingStage,
        val detectionMethod: String,
        val matchedPattern: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val findings = mutableListOf<Finding>()
    private const val MAX_FINDINGS = 100

    /**
     * Loggt ein positives Finding
     *
     * NUR aufrufen bei echten positiven Detections!
     */
    fun logFinding(finding: Finding) {
        // Store finding
        findings.add(finding)
        if (findings.size > MAX_FINDINGS) {
            findings.removeAt(0)
        }

        // Log structured output
        Log.w(TAG, formatFinding(finding))
    }

    /**
     * Convenience: Loggt Finding mit einzelnen Parametern
     */
    fun logFinding(
        text: String,
        score: Float,
        stage: GroomingStage,
        method: String,
        pattern: String? = null
    ) {
        logFinding(Finding(
            inputText = text,
            score = score,
            stage = stage,
            detectionMethod = method,
            matchedPattern = pattern
        ))
    }

    /**
     * Convenience: Loggt Finding mit Stage als String
     */
    fun logFinding(
        text: String,
        score: Float,
        stageString: String,
        method: String,
        pattern: String? = null
    ) {
        logFinding(Finding(
            inputText = text,
            score = score,
            stage = GroomingStage.fromString(stageString),
            detectionMethod = method,
            matchedPattern = pattern
        ))
    }

    /**
     * Formatiert Finding für Logcat-Ausgabe
     */
    private fun formatFinding(f: Finding): String {
        val textPreview = if (f.inputText.length > 60) {
            "${f.inputText.take(60)}..."
        } else {
            f.inputText
        }

        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
            .format(java.util.Date(f.timestamp))

        return """
            |
            |╔════════════════════════════════════════════════════════════
            |║ 🚨 GROOMING FINDING DETECTED
            |╠════════════════════════════════════════════════════════════
            |║ TEXT:       "$textPreview"
            |║ SCORE:      ${String.format("%.2f", f.score)} (${(f.score * 100).toInt()}%)
            |╠────────────────────────────────────────────────────────────
            |║ STAGE:      ${f.stage.name}
            |║ LABEL:      ${f.stage.label}
            |║ DESC:       ${f.stage.description}
            |║ SEVERITY:   ${String.format("%.1f", f.stage.baseSeverity)}
            |╠────────────────────────────────────────────────────────────
            |║ METHOD:     ${f.detectionMethod}
            |║ PATTERN:    ${f.matchedPattern ?: "—"}
            |║ TIME:       $timestamp
            |╚════════════════════════════════════════════════════════════
        """.trimMargin()
    }

    /**
     * Gibt alle gespeicherten Findings zurück
     */
    fun getFindings(): List<Finding> = findings.toList()

    /**
     * Gibt die letzten N Findings zurück
     */
    fun getRecentFindings(count: Int = 10): List<Finding> = findings.takeLast(count)

    /**
     * Exportiert alle Findings als lesbaren String
     */
    fun exportForDebug(): String {
        if (findings.isEmpty()) {
            return "No findings recorded."
        }

        return findings.mapIndexed { index, f ->
            """
            |--- Finding #${index + 1} ---
            |Text: "${f.inputText}"
            |Score: ${String.format("%.2f", f.score)}
            |Stage: ${f.stage.name} (${f.stage.label})
            |Method: ${f.detectionMethod}
            |Pattern: ${f.matchedPattern ?: "—"}
            |Time: ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(f.timestamp))}
            """.trimMargin()
        }.joinToString("\n")
    }

    /**
     * Löscht alle gespeicherten Findings
     */
    fun clearFindings() {
        findings.clear()
        Log.d(TAG, "Findings cleared")
    }

    /**
     * Gibt Anzahl der Findings zurück
     */
    fun getFindingsCount(): Int = findings.size
}
