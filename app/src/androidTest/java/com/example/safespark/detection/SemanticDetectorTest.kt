package com.example.safespark.detection

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration Tests für SemanticDetector
 *
 * Benötigt:
 * - seed_embeddings.json in assets/
 * - minilm_encoder.onnx in assets/ (optional, kann mocken)
 *
 * Tests prüfen:
 * 1. Semantic similarity detection funktioniert
 * 2. Deutsche und englische Patterns werden erkannt
 * 3. False positives werden vermieden
 * 4. Thresholds funktionieren korrekt
 */
@RunWith(AndroidJUnit4::class)
class SemanticDetectorTest {

    private lateinit var context: Context
    private lateinit var detector: SemanticDetector

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // NOTE: Dieser Test benötigt die echten Assets
        // Falls nicht vorhanden, Test überspringen
        try {
            detector = SemanticDetector(context)
        } catch (e: Exception) {
            println("⚠️ SemanticDetector nicht verfügbar, überspringe Tests")
            println("   Fehlende Assets: seed_embeddings.json, minilm_encoder.onnx")
            throw e
        }
    }

    @After
    fun teardown() {
        if (::detector.isInitialized) {
            detector.close()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SUPERVISION_CHECK Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun testDetectIntent_bist_du_alleine_SUPERVISION_CHECK_detected() {
        val result = detector.detectIntent("Bist du alleine?")

        assertThat(result.intent).isEqualTo("SUPERVISION_CHECK")
        assertThat(result.similarity).isGreaterThan(0.75f)
        assertThat(result.isRisk).isTrue()
        assertThat(result.matchedSeed).isNotNull()
    }

    @Test
    fun testDetectIntent_ist_heute_noch_jemand_bei_dir_SUPERVISION_CHECK_detected() {
        val result = detector.detectIntent("Ist heute noch jemand bei dir?")

        assertThat(result.intent).isEqualTo("SUPERVISION_CHECK")
        assertThat(result.similarity).isGreaterThan(0.75f)
        assertThat(result.isRisk).isTrue()
        println("   Matched: '${result.matchedSeed}' (${(result.similarity*100).toInt()}%)")
    }

    @Test
    fun testDetectIntent_sind_deine_eltern_zuhause_SUPERVISION_CHECK_detected() {
        val result = detector.detectIntent("Sind deine Eltern zuhause?")

        assertThat(result.intent).isEqualTo("SUPERVISION_CHECK")
        assertThat(result.isRisk).isTrue()
    }

    @Test
    fun testDetectIntent_english_are_you_alone_SUPERVISION_CHECK_detected() {
        val result = detector.detectIntent("Are you alone?")

        assertThat(result.intent).isEqualTo("SUPERVISION_CHECK")
        assertThat(result.similarity).isGreaterThan(0.75f)
        assertThat(result.isRisk).isTrue()
    }

    @Test
    fun testDetectIntent_english_are_your_parents_home_SUPERVISION_CHECK_detected() {
        val result = detector.detectIntent("Are your parents home?")

        assertThat(result.intent).isEqualTo("SUPERVISION_CHECK")
        assertThat(result.isRisk).isTrue()
    }

    // ═══════════════════════════════════════════════════════════════
    // SECRECY_REQUEST Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun testDetectIntent_sag_niemandem_davon_SECRECY_REQUEST_detected() {
        val result = detector.detectIntent("Sag niemandem davon")

        assertThat(result.intent).isEqualTo("SECRECY_REQUEST")
        assertThat(result.similarity).isGreaterThan(0.78f)
        assertThat(result.isRisk).isTrue()
    }

    @Test
    fun testDetectIntent_das_bleibt_unter_uns_SECRECY_REQUEST_detected() {
        val result = detector.detectIntent("Das bleibt unter uns")

        assertThat(result.intent).isEqualTo("SECRECY_REQUEST")
        assertThat(result.isRisk).isTrue()
    }

    @Test
    fun testDetectIntent_english_don_t_tell_anyone_SECRECY_REQUEST_detected() {
        val result = detector.detectIntent("Don't tell anyone")

        assertThat(result.intent).isEqualTo("SECRECY_REQUEST")
        assertThat(result.isRisk).isTrue()
    }

    // ═══════════════════════════════════════════════════════════════
    // PHOTO_REQUEST Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun testDetectIntent_schick_mir_ein_bild_PHOTO_REQUEST_detected() {
        val result = detector.detectIntent("Schick mir ein Bild von dir")

        assertThat(result.intent).isEqualTo("PHOTO_REQUEST")
        assertThat(result.similarity).isGreaterThan(0.80f)
        assertThat(result.isRisk).isTrue()
    }

    @Test
    fun testDetectIntent_english_send_a_picture_PHOTO_REQUEST_detected() {
        val result = detector.detectIntent("Send me a picture")

        assertThat(result.intent).isEqualTo("PHOTO_REQUEST")
        assertThat(result.isRisk).isTrue()
    }

    // ═══════════════════════════════════════════════════════════════
    // MEETING_REQUEST Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun testDetectIntent_wollen_wir_uns_treffen_MEETING_REQUEST_detected() {
        val result = detector.detectIntent("Wollen wir uns mal treffen?")

        assertThat(result.intent).isEqualTo("MEETING_REQUEST")
        assertThat(result.similarity).isGreaterThan(0.75f)
        assertThat(result.isRisk).isTrue()
    }

    @Test
    fun testDetectIntent_english_let_s_meet_MEETING_REQUEST_detected() {
        val result = detector.detectIntent("Let's meet up")

        assertThat(result.intent).isEqualTo("MEETING_REQUEST")
        assertThat(result.isRisk).isTrue()
    }

    // ═══════════════════════════════════════════════════════════════
    // FALSE POSITIVE Prevention Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun testDetectIntent_bist_du_m_de_NOT_detected() {
        val result = detector.detectIntent("Bist du müde?")

        assertThat(result.intent).isNull()
        assertThat(result.similarity).isLessThan(0.70f)
        assertThat(result.isRisk).isFalse()
    }

    @Test
    fun testDetectIntent_wie_geht_es_dir_NOT_detected() {
        val result = detector.detectIntent("Wie geht es dir?")

        assertThat(result.intent).isNull()
        assertThat(result.isRisk).isFalse()
    }

    @Test
    fun testDetectIntent_harmless_message_NOT_detected() {
        val result = detector.detectIntent("Ich habe heute viel gelernt")

        assertThat(result.intent).isNull()
        assertThat(result.isRisk).isFalse()
    }

    @Test
    fun testDetectIntent_empty_text_NOT_detected() {
        val result = detector.detectIntent("")

        assertThat(result.intent).isNull()
        assertThat(result.isRisk).isFalse()
    }

    // ═══════════════════════════════════════════════════════════════
    // Intent Score Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun testDetectIntent_returns_all_intent_scores() {
        val result = detector.detectIntent("Bist du alleine?")

        assertThat(result.allIntentScores).isNotEmpty()
        assertThat(result.allIntentScores).containsKey("SUPERVISION_CHECK")
        assertThat(result.allIntentScores).containsKey("SECRECY_REQUEST")
        assertThat(result.allIntentScores).containsKey("PHOTO_REQUEST")
        assertThat(result.allIntentScores).containsKey("MEETING_REQUEST")

        // Supervision check should have highest score
        val maxIntent = result.allIntentScores.maxByOrNull { it.value }!!
        assertThat(maxIntent.key).isEqualTo("SUPERVISION_CHECK")
    }

    // ═══════════════════════════════════════════════════════════════
    // Encoding Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun testEncode_returns_correct_dimension() {
        val embedding = detector.encode("Test message")

        assertThat(embedding).hasLength(384)  // MiniLM dimension
    }

    @Test
    fun testEncode_similar_texts_have_high_similarity() {
        val emb1 = detector.encode("Bist du alleine?")
        val emb2 = detector.encode("Bist du allein?")

        val similarity = EmbeddingUtils.cosineSimilarity(emb1, emb2)
        assertThat(similarity).isGreaterThan(0.90f)  // Very similar
    }

    @Test
    fun testEncode_different_texts_have_low_similarity() {
        val emb1 = detector.encode("Bist du alleine?")
        val emb2 = detector.encode("Das Wetter ist schön")

        val similarity = EmbeddingUtils.cosineSimilarity(emb1, emb2)
        assertThat(similarity).isLessThan(0.50f)  // Not similar
    }
}
