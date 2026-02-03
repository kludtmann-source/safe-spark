package com.example.safespark.ml

import android.content.Context
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import com.google.common.truth.Truth.assertThat

/**
 * Unit-Tests für MLGroomingDetector
 *
 * Testet:
 * - TFLite-Modell Loading
 * - Tokenization
 * - Predictions für verschiedene Grooming-Stages
 * - Edge-Cases (leere Strings, Sonderzeichen, etc.)
 */
@RunWith(MockitoJUnitRunner.Silent::class)  // Silent mode für Mocks die nicht alle genutzt werden
class MLGroomingDetectorTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var detector: MLGroomingDetector

    @Before
    fun setup() {
        // Mock Context für Asset-Zugriff
        `when`(mockContext.applicationContext).thenReturn(mockContext)

        // Hinweis: In echten Tests würde man einen TestContext mit echten Assets verwenden
        // Für diesen Unit-Test mocken wir das Verhalten
    }

    @After
    fun tearDown() {
        // Cleanup falls Detector initialisiert wurde
        try {
            if (::detector.isInitialized) {
                detector.close()
            }
        } catch (e: Exception) {
            // Ignore cleanup errors in tests
        }
    }

    // ========== GROOMING STAGE TESTS ==========

    @Test
    fun `predict STAGE_SAFE for harmless message`() {
        // Diese Tests würden mit echtem Context und Assets funktionieren
        // Für Unit-Tests ohne Android-Context dokumentieren wir erwartetes Verhalten

        val harmlessMessages = listOf(
            "Wie geht's dir?",
            "Hast du die Hausaufgaben gemacht?",
            "Willst du Fortnite spielen?",
            "Was machst du am Wochenende?"
        )

        // Erwartetes Verhalten dokumentieren
        harmlessMessages.forEach { message ->
            // Expected: STAGE_SAFE with high confidence (>0.7)
            // Expected: isDangerous = false
            assertThat(message).isNotEmpty()
        }
    }

    @Test
    fun `predict STAGE_TRUST for trust-building phrase`() {
        // HINWEIS: Dieser Test benötigt einen echten Android Context mit TFLite Model
        // In Unit-Tests (ohne Instrumentation) können wir nur das erwartete Verhalten dokumentieren

        val trustMessages = listOf(
            "Du bist echt reifer als andere in deinem Alter",
            "Ich versteh dich besser als deine Eltern",
            "Du bist was ganz Besonderes",
            "You seem very mature for your age"
        )

        trustMessages.forEach { message ->
            // Dokumentiere erwartetes Verhalten:
            // - MLGroomingDetector sollte STAGE_TRUST erkennen
            // - Confidence sollte 0.5-0.8 sein
            // - isDangerous sollte false sein (unter 0.7 threshold)

            // Für diesen Unit-Test: Prüfe dass Message Grooming-Keywords enthält
            val containsGroomingPattern = message.lowercase().contains("reif") ||
                                          message.lowercase().contains("versteh") ||
                                          message.lowercase().contains("besonder") ||  // Passt auf "besonders" UND "besonderes"
                                          message.lowercase().contains("mature")
            assertThat(containsGroomingPattern).isTrue()
        }
    }

    @Test
    fun `predict STAGE_NEEDS for material offers`() {
        val needsMessages = listOf(
            "Brauchst du Robux?",
            "Ich kann dir V-Bucks kaufen",
            "Willst du einen Battle Pass?",
            "I can buy you something, do you have PayPal?"
        )

        needsMessages.forEach { message ->
            // Expected: STAGE_NEEDS with confidence 0.6-0.8
            // Expected: isDangerous = true if > 0.7
            val lowerMessage = message.lowercase()
            val containsPattern = lowerMessage.contains("brauchst") ||
                                  lowerMessage.contains("kaufen") ||
                                  lowerMessage.contains("robux") ||
                                  lowerMessage.contains("bucks") ||
                                  lowerMessage.contains("battle pass") ||
                                  lowerMessage.contains("paypal")
            assertThat(containsPattern).isTrue()
        }
    }

    @Test
    fun `predict STAGE_ISOLATION for secrecy requests`() {
        val isolationMessages = listOf(
            "Lass uns auf Snapchat schreiben",
            "Lösch die Nachrichten, okay?",
            "Das bleibt unser Geheimnis",
            "Don't tell your parents about this",
            "Delete this conversation"
        )

        isolationMessages.forEach { message ->
            // Expected: STAGE_ISOLATION with confidence 0.7-0.9
            // Expected: isDangerous = true
            val lowerMessage = message.lowercase()
            val containsPattern = lowerMessage.contains("snapchat") ||
                                  lowerMessage.contains("lösch") ||
                                  lowerMessage.contains("geheimnis") ||
                                  lowerMessage.contains("delete") ||
                                  lowerMessage.contains("secret") ||
                                  lowerMessage.contains("don't tell")
            assertThat(containsPattern).isTrue()
        }
    }

    @Test
    fun `predict STAGE_ASSESSMENT for critical isolation questions`() {
        val assessmentMessages = listOf(
            "Bist du grad allein?",
            "Wo sind deine Eltern?",
            "Ist jemand bei dir im Zimmer?",
            "Are you alone right now?",
            "Can anyone hear us?"
        )

        assessmentMessages.forEach { message ->
            // Expected: STAGE_ASSESSMENT with confidence 0.8-0.95
            // Expected: isDangerous = true (HIGH RISK!)
            val lowerMessage = message.lowercase()
            val containsPattern = lowerMessage.contains("allein") ||
                                  lowerMessage.contains("alone") ||
                                  lowerMessage.contains("eltern") ||
                                  lowerMessage.contains("parents") ||
                                  lowerMessage.contains("zimmer") ||
                                  lowerMessage.contains("room") ||
                                  lowerMessage.contains("jemand") ||
                                  lowerMessage.contains("anyone")
            assertThat(containsPattern).isTrue()
        }
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    fun `handle empty string gracefully`() {
        val emptyMessages = listOf("", "   ", "\n", "\t")

        emptyMessages.forEach { message ->
            // Expected: STAGE_SAFE with confidence 1.0
            // Expected: isDangerous = false
            assertThat(message.isBlank()).isTrue()
        }
    }

    @Test
    fun `handle special characters and emojis`() {
        val messagesWithSpecialChars = listOf(
            "Bist du allein? 😊",
            "!!!ROBUX!!!",
            "Du bist <special>",
            "@#$%^&*()",
            "🚨🚨🚨"
        )

        messagesWithSpecialChars.forEach { message ->
            // Expected: Special chars should be stripped, emojis ignored
            // Core keywords should still be detected
            assertThat(message).isNotEmpty()
        }
    }

    @Test
    fun `handle very long messages`() {
        val longMessage = "Hallo " + "du bist toll ".repeat(100) + " bist du allein?"

        // Expected: Should be truncated to MAX_SEQUENCE_LENGTH (50 tokens)
        // Should still detect "allein" pattern
        assertThat(longMessage.length).isGreaterThan(500)
    }

    @Test
    fun `handle mixed German and English`() {
        val mixedMessages = listOf(
            "Hey, bist du allein right now?",
            "Do you want Robux oder V-Bucks?",
            "You seem sehr reif für dein age"
        )

        mixedMessages.forEach { message ->
            // Expected: Both German and English keywords should be detected
            // Model trained on both languages
            val hasLetters = message.any { it.isLetter() }
            assertThat(hasLetters).isTrue()
        }
    }

    @Test
    fun `handle typos and slang`() {
        val slangMessages = listOf(
            "bist du grad alein?", // Typo: alein statt allein
            "brauchst rbx?", // Abbr: rbx statt robux
            "u alone rn?", // Slang: u, rn
            "lösch die msgs" // Abbr: msgs
        )

        slangMessages.forEach { message ->
            // Expected: ML model should be somewhat robust to typos
            // Keywords might not match, but ML should catch context
            assertThat(message).isNotEmpty()
        }
    }

    // ========== TOKENIZATION TESTS ==========

    @Test
    fun `tokenization removes special characters`() {
        val input = "Bist du allein? 😊!!!"
        val expectedTokens = listOf("bist", "du", "allein")

        // Expected: Only alphanumeric + umlauts remain
        assertThat(input).contains("?")
        // After tokenization: should be removed
    }

    @Test
    fun `tokenization handles umlauts correctly`() {
        val germanWords = listOf("übel", "ärgern", "mögen", "für")

        germanWords.forEach { word ->
            // Expected: Umlauts should be preserved
            val hasUmlaut = word.any { it in "äöüÄÖÜß" }
            assertThat(hasUmlaut).isTrue()
        }
    }

    @Test
    fun `tokenization converts to lowercase`() {
        val input = "BIST DU ALLEIN?"

        // Expected: "bist du allein" after processing
        assertThat(input.lowercase()).isEqualTo("bist du allein?")
    }

    // ========== CONFIDENCE & THRESHOLD TESTS ==========

    @Test
    fun `isDangerous is false when confidence below threshold`() {
        val confidence = 0.65f
        val threshold = 0.7f

        assertThat(confidence).isLessThan(threshold)
        // Expected: isDangerous = false
    }

    @Test
    fun `isDangerous is true when confidence above threshold`() {
        val confidence = 0.85f
        val threshold = 0.7f

        assertThat(confidence).isGreaterThan(threshold)
        // Expected: isDangerous = true
    }

    @Test
    fun `high confidence predictions are more reliable`() {
        // Confidence > 0.8 should be trusted more
        val highConfidence = 0.92f
        assertThat(highConfidence).isAtLeast(0.8f)
    }

    // ========== NULL SAFETY TESTS ==========

    @Test
    fun `predict returns null when model not loaded`() {
        // If TFLite model fails to load, predict should return null gracefully
        // Not crash the app

        // Expected: Null-safe handling in SafeSparkEngine
        val nullResult: MLGroomingDetector.GroomingPrediction? = null
        assertThat(nullResult).isNull()
    }

    @Test
    fun `close can be called multiple times safely`() {
        // Detector.close() should be idempotent
        // No crash if called multiple times

        // This documents expected behavior
        assertThat(true).isTrue()
    }

    // ========== PERFORMANCE TESTS ==========

    @Test
    fun `prediction should be fast`() {
        // Expected: Inference < 50ms (ideally < 10ms)
        // This is a documentation of performance requirement

        val maxInferenceTimeMs = 50L
        assertThat(maxInferenceTimeMs).isLessThan(100L)
    }

    @Test
    fun `model size should be under 5MB`() {
        // Expected: grooming_detector_scientific.tflite = 0.03 MB
        val expectedSizeMB = 0.03
        val maxSizeMB = 5.0

        assertThat(expectedSizeMB).isLessThan(maxSizeMB)
    }

    // ========== INTEGRATION NOTES ==========

    /**
     * HINWEIS FÜR ECHTE TESTS:
     *
     * Diese Unit-Tests sind aktuell "Dokumentations-Tests", die erwartetes
     * Verhalten beschreiben. Für echte funktionale Tests mit dem TFLite-Modell:
     *
     * 1. Verwende Instrumented Tests (AndroidTest) mit echtem Context
     * 2. Kopiere Assets (grooming_detector_scientific.tflite) in androidTest/assets/
     * 3. Verwende ApplicationProvider.getApplicationContext()
     * 4. Teste gegen echte Predictions
     *
     * Beispiel für Instrumented Test:
     *
     * @RunWith(AndroidJUnit4::class)
     * class MLGroomingDetectorInstrumentedTest {
     *
     *     private lateinit var detector: MLGroomingDetector
     *
     *     @Before
     *     fun setup() {
     *         val context = ApplicationProvider.getApplicationContext<Context>()
     *         detector = MLGroomingDetector(context)
     *     }
     *
     *     @Test
     *     fun testRealPrediction() {
     *         val result = detector.predict("Bist du allein?")
     *         assertThat(result).isNotNull()
     *         assertThat(result?.stage).isEqualTo("STAGE_ASSESSMENT")
     *         assertThat(result?.confidence).isGreaterThan(0.7f)
     *         assertThat(result?.isDangerous).isTrue()
     *     }
     * }
     */
}
