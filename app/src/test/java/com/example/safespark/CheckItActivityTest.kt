package com.example.safespark

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for CheckItActivity helper functions.
 * Tests the risk level determination and stage translation logic.
 */
class CheckItActivityTest {

    /**
     * Test for determineRiskLevel() method.
     * Verifies that float scores are correctly mapped to RiskCategory enum values.
     */
    @Test
    fun testDetermineRiskLevel_SafeRange() {
        // Score < 0.3f should be SAFE
        assertEquals(RiskCategory.SAFE, RiskLevelUtils.determineRiskLevel(0.0f))
        assertEquals(RiskCategory.SAFE, RiskLevelUtils.determineRiskLevel(0.1f))
        assertEquals(RiskCategory.SAFE, RiskLevelUtils.determineRiskLevel(0.29f))
    }

    @Test
    fun testDetermineRiskLevel_CautionRange() {
        // Score >= 0.3f && < 0.6f should be CAUTION
        assertEquals(RiskCategory.CAUTION, RiskLevelUtils.determineRiskLevel(0.3f))
        assertEquals(RiskCategory.CAUTION, RiskLevelUtils.determineRiskLevel(0.45f))
        assertEquals(RiskCategory.CAUTION, RiskLevelUtils.determineRiskLevel(0.59f))
    }

    @Test
    fun testDetermineRiskLevel_DangerRange() {
        // Score >= 0.6f should be DANGER
        assertEquals(RiskCategory.DANGER, RiskLevelUtils.determineRiskLevel(0.6f))
        assertEquals(RiskCategory.DANGER, RiskLevelUtils.determineRiskLevel(0.85f))
        assertEquals(RiskCategory.DANGER, RiskLevelUtils.determineRiskLevel(1.0f))
    }

    @Test
    fun testDetermineRiskLevel_EdgeCases() {
        // Test exact boundaries
        assertEquals(RiskCategory.SAFE, RiskLevelUtils.determineRiskLevel(0.29999f))
        assertEquals(RiskCategory.CAUTION, RiskLevelUtils.determineRiskLevel(0.3f))
        assertEquals(RiskCategory.CAUTION, RiskLevelUtils.determineRiskLevel(0.59999f))
        assertEquals(RiskCategory.DANGER, RiskLevelUtils.determineRiskLevel(0.6f))
    }

    /**
     * Test for translateStage() method.
     * Verifies that stage codes are correctly translated to German.
     */
    @Test
    fun testTranslateStage_KnownStages() {
        assertEquals("Vertrauensaufbau", StageTranslator.translateStage("TRUST"))
        assertEquals("Vertrauensaufbau", StageTranslator.translateStage("STAGE_TRUST"))
        
        assertEquals("Isolierung", StageTranslator.translateStage("ISOLATION"))
        assertEquals("Isolierung", StageTranslator.translateStage("STAGE_ISOLATION"))
        
        assertEquals("Situationscheck", StageTranslator.translateStage("ASSESSMENT"))
        assertEquals("Situationscheck", StageTranslator.translateStage("STAGE_ASSESSMENT"))
        
        assertEquals("Desensibilisierung", StageTranslator.translateStage("DESENSITIZATION"))
        assertEquals("Desensibilisierung", StageTranslator.translateStage("STAGE_DESENSITIZATION"))
        
        assertEquals("Sexuelle Inhalte", StageTranslator.translateStage("SEXUAL"))
        assertEquals("Sexuelle Inhalte", StageTranslator.translateStage("STAGE_SEXUAL"))
        
        assertEquals("Geheimhaltung", StageTranslator.translateStage("MAINTENANCE"))
        assertEquals("Geheimhaltung", StageTranslator.translateStage("STAGE_MAINTENANCE"))
        assertEquals("Geheimhaltung", StageTranslator.translateStage("SECRECY"))
        
        assertEquals("Geschenke/Hilfe", StageTranslator.translateStage("NEEDS"))
        assertEquals("Geschenke/Hilfe", StageTranslator.translateStage("STAGE_NEEDS"))
        assertEquals("Geschenke/Hilfe", StageTranslator.translateStage("GIFT"))
    }

    @Test
    fun testTranslateStage_CaseInsensitive() {
        // Test lowercase variants
        assertEquals("Vertrauensaufbau", StageTranslator.translateStage("trust"))
        assertEquals("Isolierung", StageTranslator.translateStage("isolation"))
        assertEquals("Situationscheck", StageTranslator.translateStage("assessment"))
        
        // Test mixed case
        assertEquals("Vertrauensaufbau", StageTranslator.translateStage("TrUsT"))
        assertEquals("Isolierung", StageTranslator.translateStage("IsoLaTion"))
    }

    @Test
    fun testTranslateStage_UnknownStage() {
        // Unknown stages should return the original code
        assertEquals("UNKNOWN_STAGE", StageTranslator.translateStage("UNKNOWN_STAGE"))
        assertEquals("CUSTOM", StageTranslator.translateStage("CUSTOM"))
        assertEquals("", StageTranslator.translateStage(""))
    }
}
