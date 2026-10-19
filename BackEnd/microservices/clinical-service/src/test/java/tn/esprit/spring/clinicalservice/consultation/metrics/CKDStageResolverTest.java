package tn.esprit.spring.clinicalservice.consultation.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import tn.esprit.spring.clinicalservice.consultation.metrics.CKDStageResolver.CKDStage;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CKDStageResolver — stades KDIGO (valeurs limites)")
class CKDStageResolverTest {

    private final CKDStageResolver resolver = new CKDStageResolver();

    @ParameterizedTest(name = "eGFR {0} → {1}")
    @CsvSource({
            "120, STAGE_1", "90.1, STAGE_1", "90, STAGE_2", "60, STAGE_2",
            "59.9, STAGE_3A", "45, STAGE_3A", "44.9, STAGE_3B", "30, STAGE_3B",
            "29.9, STAGE_4", "15, STAGE_4", "14.9, STAGE_5", "0, STAGE_5"
    })
    void resoutLeStadeAuxValeursLimites(double egfr, CKDStage expected) {
        assertEquals(expected, resolver.resolveCKDStage(egfr));
    }

    @Test
    void referralEtDialyse() {
        assertFalse(resolver.requiresNephrologistReferral(CKDStage.STAGE_2));
        assertTrue(resolver.requiresNephrologistReferral(CKDStage.STAGE_3A));
        assertFalse(resolver.dialysisRequired(CKDStage.STAGE_3B));
        assertTrue(resolver.dialysisRequired(CKDStage.STAGE_4));
        assertTrue(resolver.dialysisRequired(CKDStage.STAGE_5));
    }

    @ParameterizedTest
    @EnumSource(CKDStage.class)
    void chaqueStadeExposeSesAttributsUi(CKDStage stage) {
        assertTrue(stage.getFullName().startsWith("CKD Stage " + stage.getSeverity()));
        assertNotNull(stage.getCssClass());
        assertTrue(stage.getColorHex().startsWith("#"));
        assertTrue(stage.getFollowUpIntervalWeeks() > 0);
        assertNotNull(stage.getDescription());
        assertNotNull(stage.getClinicalRecommendation());
        assertNotNull(stage.getEGFRRange());
    }

    @Test
    void suiviPlusRapprocheQuandLeStadeSAggrave() {
        assertEquals(52, CKDStage.STAGE_1.getFollowUpIntervalWeeks());
        assertEquals(12, CKDStage.STAGE_3B.getFollowUpIntervalWeeks());
        assertEquals(6, CKDStage.STAGE_4.getFollowUpIntervalWeeks());
        assertEquals(2, CKDStage.STAGE_5.getFollowUpIntervalWeeks());
        assertEquals("danger", CKDStage.STAGE_5.getCssClass());
        assertEquals("warning", CKDStage.STAGE_3A.getCssClass());
        assertEquals("success", CKDStage.STAGE_2.getCssClass());
    }

    @Test
    void progressionRapide() {
        assertFalse(CKDStage.isRapidProgression(null, CKDStage.STAGE_5));
        assertFalse(CKDStage.isRapidProgression(CKDStage.STAGE_1, CKDStage.STAGE_2));
        assertTrue(CKDStage.isRapidProgression(CKDStage.STAGE_1, CKDStage.STAGE_3A));
        assertTrue(CKDStage.isRapidProgression(CKDStage.STAGE_4, CKDStage.STAGE_4));
    }
}
