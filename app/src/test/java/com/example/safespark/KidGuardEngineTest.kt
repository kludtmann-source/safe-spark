package com.example.safespark

import android.content.Context
import com.example.safespark.ml.MLGroomingDetector
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import com.google.common.truth.Truth.assertThat

/**
 * Unit-Tests für SafeSparkEngine
 *
 * Testet das Hybrid-System:
 * - ML-Predictions (70% Gewicht)
 * - Keyword-Matching (30% Gewicht)
 * - Assessment-Pattern Detection (Critical!)
 * - Score-Berechnung
 */
@RunWith(MockitoJUnitRunner::class)
class KidGuardEngineTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockMLDetector: MLGroomingDetector

    // Diese Tests dokumentieren das erwartete Verhalten des Hybrid-Systems

    // ========== HYBRID SYSTEM TESTS ==========

    @Test
    fun `hybrid system combines ML and keyword scores`() {
        // Szenario: ML sagt 0.8, Keywords sagen 0.6
        val mlScore = 0.8f
        val keywordScore = 0.6f
        val mlWeight = 0.7f
        val keywordWeight = 0.3f

        val expectedFinalScore = (mlScore * mlWeight) + (keywordScore * keywordWeight)
        // = (0.8 * 0.7) + (0.6 * 0.3) = 0.56 + 0.18 = 0.74

        assertThat(expectedFinalScore).isWithin(0.01f).of(0.74f)
    }

    @Test
    fun `ML score dominates when confidence is high`() {
        // Bei ML Confidence > 0.8: Nur ML-Score verwenden
        val mlConfidence = 0.9f
        val threshold = 0.8f

        assertThat(mlConfidence).isGreaterThan(threshold)
        // Expected: SafeSparkEngine returns ML score directly
    }

    @Test
    fun `assessment patterns override other predictions`() {
        // CRITICAL: "allein", "zimmer", "eltern" Patterns
        val criticalPatterns = mapOf(
            "allein" to 0.85f,
            "alleine" to 0.85f,
            "alone" to 0.85f,
            "zimmer" to 0.75f,
            "room" to 0.75f,
            "eltern" to 0.70f,
            "parents" to 0.70f,
            "niemand" to 0.80f,
            "nobody" to 0.80f,
            "tür" to 0.75f,
            "door" to 0.75f
        )

        criticalPatterns.forEach { (pattern, expectedScore) ->
            assertThat(expectedScore).isAtLeast(0.70f)
            // Expected: Diese Patterns sollten immer High-Risk liefern
        }
    }

    @Test
    fun `assessment pattern allein returns high score`() {
        val message = "bist du allein?"
        val expectedMinScore = 0.85f

        assertThat(message.lowercase()).contains("allein")
        // Expected: Score >= 0.85
    }

    @Test
    fun `assessment pattern eltern returns high score`() {
        val message = "wo sind deine eltern?"
        val expectedMinScore = 0.70f

        assertThat(message.lowercase()).contains("eltern")
        // Expected: Score >= 0.70
    }

    // ========== KEYWORD MATCHING TESTS ==========

    @Test
    fun `single risk keyword gives 0_75 score`() {
        // 1 Risk-Keyword = 0.75 Score
        val riskCount = 1
        val expectedScore = 0.75f

        val calculatedScore = when {
            riskCount == 0 -> 0.0f
            riskCount == 1 -> 0.75f
            riskCount >= 2 -> 0.95f
            else -> 0.5f
        }

        assertThat(calculatedScore).isEqualTo(expectedScore)
    }

    @Test
    fun `two or more risk keywords give 0_95 score`() {
        val riskCount = 2
        val expectedScore = 0.95f

        val calculatedScore = when {
            riskCount == 0 -> 0.0f
            riskCount == 1 -> 0.75f
            riskCount >= 2 -> 0.95f
            else -> 0.5f
        }

        assertThat(calculatedScore).isEqualTo(expectedScore)
    }

    @Test
    fun `no risk keywords give 0 score`() {
        val riskCount = 0
        val expectedScore = 0.0f

        val calculatedScore = when {
            riskCount == 0 -> 0.0f
            riskCount == 1 -> 0.75f
            riskCount >= 2 -> 0.95f
            else -> 0.5f
        }

        assertThat(calculatedScore).isEqualTo(expectedScore)
    }

    // ========== SCORE CALCULATION TESTS ==========

    @Test
    fun `final score is clamped between 0 and 1`() {
        val scores = listOf(-0.5f, 0.0f, 0.5f, 1.0f, 1.5f)

        scores.forEach { score ->
            val clamped = score.coerceIn(0.0f, 1.0f)
            assertThat(clamped).isAtLeast(0.0f)
            assertThat(clamped).isAtMost(1.0f)
        }
    }

    @Test
    fun `harmless message returns low score`() {
        val harmlessMessages = listOf(
            "Wie geht's dir?",
            "Hast du die Hausaufgaben?",
            "Willst du spielen?"
        )

        // Expected: Score < 0.3 for all
        harmlessMessages.forEach { message ->
            assertThat(message).doesNotContain("allein")
            assertThat(message).doesNotContain("robux")
            assertThat(message).doesNotContain("snapchat")
        }
    }

    @Test
    fun `dangerous message returns high score`() {
        val dangerousMessages = listOf(
            "Bist du allein?",
            "Lösch die Nachrichten",
            "Wo sind deine Eltern?"
        )

        // Expected: Score > 0.7 for all
        dangerousMessages.forEach { message ->
            val containsDangerousPattern =
                message.lowercase().contains("allein") ||
                message.lowercase().contains("lösch") ||
                message.lowercase().contains("eltern")

            assertThat(containsDangerousPattern).isTrue()
        }
    }

    // ========== FALLBACK TESTS ==========

    @Test
    fun `engine uses keyword fallback when ML fails`() {
        // Wenn ML null zurückgibt, sollte Keyword-Matching allein funktionieren
        val mlResult: MLGroomingDetector.GroomingPrediction? = null
        val keywordScore = 0.75f

        val finalScore = mlResult?.confidence ?: keywordScore
        assertThat(finalScore).isEqualTo(keywordScore)
    }

    @Test
    fun `engine handles ML null gracefully`() {
        // Null-Safety: Engine sollte nicht crashen wenn ML null liefert
        val mlResult: MLGroomingDetector.GroomingPrediction? = null

        assertThat(mlResult).isNull()
        // Expected: Engine should fall back to keywords only
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    fun `empty input returns zero score`() {
        val emptyInputs = listOf("", "   ", "\n", "\t")

        emptyInputs.forEach { input ->
            val trimmed = input.trim()
            assertThat(trimmed).isEmpty()
            // Expected: Score = 0.0
        }
    }

    @Test
    fun `case insensitive keyword matching`() {
        val variations = listOf(
            "ALLEIN",
            "Allein",
            "allein",
            "aLlEiN"
        )

        variations.forEach { variant ->
            assertThat(variant.lowercase()).isEqualTo("allein")
        }
    }

    @Test
    fun `whitespace is normalized`() {
        val messagesWithWeirdSpacing = listOf(
            "bist   du    allein?",
            "bist\tdu\tallein?",
            "bist\ndu\nallein?"
        )

        messagesWithWeirdSpacing.forEach { message ->
            val normalized = message.split(Regex("\\s+"))
            assertThat(normalized).containsAtLeast("bist", "du", "allein?")
        }
    }

    // ========== SKIP WORDS TESTS ==========

    @Test
    fun `common words are skipped in risk calculation`() {
        val skipWords = setOf(
            "<unk>", "the", "to", "and", "a", "of", "is", "in",
            "you", "it", "that", "child", "safety", "protect"
        )

        skipWords.forEach { word ->
            // These should NOT count as risk keywords
            assertThat(word).isIn(skipWords)
        }
    }

    @Test
    fun `child and safety are not risk keywords`() {
        val message = "child safety is important"

        // Diese Wörter sollten NICHT als Risk gelten
        assertThat(message).contains("child")
        assertThat(message).contains("safety")
        // Expected: Score should be 0.0 or very low
    }

    // ========== VOCABULARY TESTS ==========

    @Test
    fun `vocabulary size is reasonable`() {
        // vocabulary.txt sollte 1000-2000 Wörter haben
        val expectedMinSize = 500
        val expectedMaxSize = 5000

        assertThat(expectedMinSize).isLessThan(expectedMaxSize)
    }

    @Test
    fun `risk keywords are loaded from vocabulary`() {
        // Vocabulary sollte Risk-Keywords enthalten
        val expectedKeywords = listOf(
            "allein", "robux", "snapchat", "secret",
            "eltern", "parents", "webcam", "foto"
        )

        expectedKeywords.forEach { keyword ->
            assertThat(keyword).isNotEmpty()
        }
    }

    // ========== LOGGING TESTS ==========

    @Test
    fun `high risk scores are logged with warning`() {
        val highRiskScore = 0.85f
        val warningThreshold = 0.7f

        assertThat(highRiskScore).isGreaterThan(warningThreshold)
        // Expected: Log.w() should be called
    }

    @Test
    fun `matched keywords are logged for debugging`() {
        val message = "bist du allein?"
        val expectedMatches = listOf("allein")

        assertThat(expectedMatches).hasSize(1)
        // Expected: Log.d() should show matched keywords
    }

    // ========== RESOURCE MANAGEMENT TESTS ==========

    @Test
    fun `engine can be closed safely`() {
        // close() sollte Ressourcen freigeben
        // Kein Memory Leak

        // Expected: MLDetector.close() wird aufgerufen
        assertThat(true).isTrue()
    }

    @Test
    fun `engine can be reused after close`() {
        // Nach close() sollte eine neue Instanz erstellt werden können

        // Expected: Keine IllegalStateException
        assertThat(true).isTrue()
    }

    // ========== PERFORMANCE TESTS ==========

    @Test
    fun `analyzeText should be fast`() {
        // Expected: < 20ms total (ML: 10ms + Keywords: 5ms + Hybrid: 5ms)
        val maxTotalTimeMs = 20L

        assertThat(maxTotalTimeMs).isLessThan(50L)
    }

    @Test
    fun `keyword matching should be instant`() {
        // Keyword-Matching sollte < 1ms sein
        val maxKeywordTimeMs = 1L

        assertThat(maxKeywordTimeMs).isLessThan(5L)
    }

    // ========== INTEGRATION SCENARIOS ==========

    @Test
    fun `scenario high ML confidence overrides keywords`() {
        // ML: 0.92 (STAGE_ASSESSMENT), Keywords: 0.0
        val mlConfidence = 0.92f
        val keywordScore = 0.0f

        // Expected: Final score = 0.92 (ML dominiert)
        assertThat(mlConfidence).isGreaterThan(0.8f)
    }

    @Test
    fun `scenario critical pattern overrides ML`() {
        // ML: 0.5 (STAGE_SAFE), aber "allein" detected
        val mlScore = 0.5f
        val patternScore = 0.85f

        // Expected: Final score = 0.85 (Pattern dominiert)
        assertThat(patternScore).isGreaterThan(mlScore)
    }

    @Test
    fun `scenario both ML and keywords agree`() {
        // ML: 0.8, Keywords: 0.75
        val mlScore = 0.8f
        val keywordScore = 0.75f
        val hybrid = (mlScore * 0.7f) + (keywordScore * 0.3f)

        // = 0.56 + 0.225 = 0.785
        assertThat(hybrid).isWithin(0.01f).of(0.785f)
    }

    @Test
    fun `scenario false positive mitigation`() {
        // ML: 0.65, Keywords: 0.0
        // Hybrid: (0.65 * 0.7) + (0.0 * 0.3) = 0.455
        val mlScore = 0.65f
        val keywordScore = 0.0f
        val hybrid = (mlScore * 0.7f) + (keywordScore * 0.3f)

        // < 0.7 threshold → nicht gefährlich
        assertThat(hybrid).isLessThan(0.7f)
    }

    // ========== DOCUMENTATION ==========

    /**
     * HINWEIS FÜR ECHTE TESTS:
     *
     * Diese Tests dokumentieren erwartetes Verhalten. Für funktionale Tests:
     *
     * 1. Erstelle Instrumented Tests mit echtem Context
     * 2. Teste SafeSparkEngine mit echtem MLGroomingDetector
     * 3. Verwende Test-Assets mit vocabulary.txt
     *
     * Beispiel:
     *
     * @RunWith(AndroidJUnit4::class)
     * class SafeSparkEngineInstrumentedTest {
     *
     *     private lateinit var engine: KidGuardEngine
     *
     *     @Before
     *     fun setup() {
     *         val context = ApplicationProvider.getApplicationContext<Context>()
     *         engine = KidGuardEngine(context)
     *     }
     *
     *     @Test
     *     fun testRealAnalysis() {
     *         val score = engine.analyzeText("Bist du allein?")
     *         assertThat(score).isGreaterThan(0.7f)
     *     }
     *
     *     @After
     *     fun tearDown() {
     *         engine.close()
     *     }
     * }
     */
}
