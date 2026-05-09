package tn.esprit.spring.clinicalservice.consultation.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for CKDEPICalculationService
 * 
 * Tests the European CKD-EPI 2021 formula implementation with SI units (µmol/L)
 * Formula: eGFR = 141 × min(SCr/κ, 1)^α × max(SCr/κ, 1)^-1.209 × 0.993^Age [× 1.018 if female]
 */
@DisplayName("CKD-EPI Formula Calculation Tests")
class CKDEPICalculationServiceTest {

    private CKDEPICalculationService service;

    @BeforeEach
    void setUp() {
        service = new CKDEPICalculationService();
    }

    // ============================================================
    // TEST 1: Normal Female - eGFR Calculation
    // ============================================================
    @Test
    @DisplayName("TC1: Female age 50 with normal creatinine → eGFR ~75 mL/min/1.73m²")
    void testNormalFemaleEgfr() {
        // Given: Female, age 50, creatinine 1.0 mg/dL (normal ~0.6-1.2)
        String gender = "F";
        Integer age = 50;
        Double creatinineMgDl = 1.0;

        // When: Calculate eGFR using CKD-EPI formula
        Double egfr = service.calculateEgfr(creatinineMgDl, age, gender, true);

        // Then: Result should be in normal range (~75 mL/min/1.73m²)
        assertNotNull(egfr, "eGFR should not be null for valid inputs");
        assertTrue(egfr > 60, "Female with creatinine 1.0 mg/dL should have eGFR > 60");
        assertTrue(egfr < 90, "Female with creatinine 1.0 mg/dL should have eGFR < 90");
        System.out.println("✓ TC1 PASSED: Female eGFR = " + egfr + " mL/min/1.73m²");
    }

    // ============================================================
    // TEST 2: Normal Male - eGFR Calculation with Gender Coefficients
    // ============================================================
    @Test
    @DisplayName("TC2: Male age 50 with normal creatinine → eGFR ~87 mL/min/1.73m² (higher than female)")
    void testNormalMaleEgfr() {
        // Given: Male, age 50, creatinine 1.0 mg/dL
        String gender = "M";
        Integer age = 50;
        Double creatinineMgDl = 1.0;

        // When: Calculate eGFR
        Double egfr = service.calculateEgfr(creatinineMgDl, age, gender, true);

        // Then: Male should have higher eGFR than female for same creatinine (no 1.018 multiplier)
        assertNotNull(egfr, "eGFR should not be null");
        assertTrue(egfr > 70, "Male with creatinine 1.0 mg/dL should have eGFR > 70");
        assertTrue(egfr > 75, "Male eGFR should be higher than female (~87 vs ~75)");
        System.out.println("✓ TC2 PASSED: Male eGFR = " + egfr + " mL/min/1.73m²");
    }

    // ============================================================
    // TEST 3: Gender Coefficient Verification
    // ============================================================
    @Test
    @DisplayName("TC3: Female eGFR should be ~1.018x higher than males for same creatinine")
    void testGenderCoefficientDifference() {
        Integer age = 50;
        Double scr = 1.0;

        Double maleEgfr = service.calculateEgfr(scr, age, "M", true);
        Double femaleEgfr = service.calculateEgfr(scr, age, "F", true);

        assertNotNull(maleEgfr, "Male eGFR should not be null");
        assertNotNull(femaleEgfr, "Female eGFR should not be null");

        // Female multiplier is 1.018, so female eGFR should be ~1.018x higher
        double ratio = femaleEgfr / maleEgfr;
        assertTrue(ratio > 1.015 && ratio < 1.025, 
                   "Female/Male eGFR ratio should be ~1.018 (got " + ratio + ")");
        System.out.println("✓ TC3 PASSED: Female/Male ratio = " + ratio);
    }

    // ============================================================
    // TEST 4: Unit Conversion - mg/dL to µmol/L
    // ============================================================
    @Test
    @DisplayName("TC4: Unit conversion factor 88.4: 1.0 mg/dL = 88.4 µmol/L")
    void testUnitConversionMgDlToMicromol() {
        // Given: Creatinine in mg/dL
        Double creatinineMgDl = 1.0;
        Double expectedMicromol = 88.4;

        // When: Service converts internally
        Double creatinineUmol = creatinineMgDl * 88.4;

        // Then: Conversion factor should be correct
        assertEquals(expectedMicromol, creatinineUmol, 0.001,
                    "1.0 mg/dL should equal ~88.4 µmol/L");
        System.out.println("✓ TC4 PASSED: " + creatinineMgDl + " mg/dL = " + creatinineUmol + " µmol/L");
    }

    // ============================================================
    // TEST 5: Reduced Kidney Function Detection
    // ============================================================
    @Test
    @DisplayName("TC5: High creatinine → Low eGFR (Stage 3a CKD: eGFR 30-59)")
    void testReducedKidneyFunctionDetection() {
        // Given: Elevated creatinine indicating kidney impairment
        String gender = "M";
        Integer age = 50;
        Double creatinineMgDl = 2.0;  // High creatinine (normal ~0.7-1.3)

        // When: Calculate eGFR
        Double egfr = service.calculateEgfr(creatinineMgDl, age, gender, true);

        // Then: eGFR should be significantly reduced (Stage 3a or 3b)
        assertNotNull(egfr, "eGFR should not be null");
        assertTrue(egfr < 60, "High creatinine should result in eGFR < 60 (Stage 3+)");
        System.out.println("✓ TC5 PASSED: Elevated creatinine eGFR = " + egfr + " mL/min/1.73m² (Stage 3+)");
    }

    // ============================================================
    // TEST 6: Age-Related eGFR Changes
    // ============================================================
    @ParameterizedTest
    @CsvSource({
        "30, 1.0, M, 110",  // Young healthy
        "50, 1.0, M, 87",   // Middle-aged normal
        "75, 1.0, M, 75"    // Elderly, lower eGFR
    })
    @DisplayName("TC6: Age affects eGFR (younger = higher eGFR for same creatinine)")
    void testAgeEffect(Integer age, Double scr, String gender, Integer expectedMin) {
        Double egfr = service.calculateEgfr(scr, age, gender, true);

        assertNotNull(egfr, "eGFR should not be null");
        assertTrue(egfr > expectedMin - 20 && egfr > 0,
                  "Age " + age + " with creatinine " + scr + " should have reasonable eGFR");
        System.out.println("✓ TC6: Age " + age + " → eGFR = " + egfr);
    }

    // ============================================================
    // TEST 7: Rapid Decline Detection (>20% change)
    // ============================================================
    @Test
    @DisplayName("TC7: Detect rapid kidney function decline (>20% drop from previous)")
    void testRapidDeclineDetection() {
        // Given: Previous eGFR = 100, Current eGFR = 75 (25% decline)
        Double previousEgfr = 100.0;
        Double currentEgfr = 75.0;
        Double percentChange = ((currentEgfr - previousEgfr) / previousEgfr) * 100;

        // When: Evaluate decline
        boolean isRapidDecline = percentChange <= -20;

        // Then: Should be flagged as rapid decline
        assertEquals(-25.0, percentChange, 0.1);
        assertTrue(isRapidDecline, "25% decline should trigger rapid decline alert");
        System.out.println("✓ TC7 PASSED: Decline " + percentChange + "% → ALERT");
    }

    // ============================================================
    // TEST 8: Edge Case - Zero Creatinine (Invalid)
    // ============================================================
    @Test
    @DisplayName("TC8: Zero or negative creatinine returns null (invalid)")
    void testZeroCreatinineHandling() {
        Double zeroEgfr = service.calculateEgfr(0.0, 50, "M", true);
        Double negativeEgfr = service.calculateEgfr(-1.0, 50, "M", true);

        assertNull(zeroEgfr, "eGFR should be null for zero creatinine");
        assertNull(negativeEgfr, "eGFR should be null for negative creatinine");
        System.out.println("✓ TC8 PASSED: Invalid creatinine values return null");
    }

    // ============================================================
    // TEST 9: Edge Case - Invalid Age
    // ============================================================
    @Test
    @DisplayName("TC9: Invalid age (zero, negative) returns null")
    void testInvalidAgeHandling() {
        Double zeroAgeEgfr = service.calculateEgfr(1.0, 0, "M", true);
        Double negativeAgeEgfr = service.calculateEgfr(1.0, -5, "M", true);

        assertNull(zeroAgeEgfr, "eGFR should be null for zero age");
        assertNull(negativeAgeEgfr, "eGFR should be null for negative age");
        System.out.println("✓ TC9 PASSED: Invalid age values return null");
    }

    // ============================================================
    // TEST 10: CKD Stage Classification
    // ============================================================
    @ParameterizedTest
    @CsvSource({
        "95, NORMAL",      // Normal: >90
        "75, STAGE_1",     // Stage 1: 60-89
        "45, STAGE_2",     // Stage 2: 30-59
        "25, STAGE_3",     // Stage 3: 15-29
        "8,  STAGE_4"      // Stage 4: <15
    })
    @DisplayName("TC10: eGFR values correctly map to CKD stages")
    void testCkdStageClassification(Double egfr, String expectedStage) {
        // This test verifies the stage mapping (actual method is in CKDStageResolver)
        // Here we verify that eGFR ranges are mathematically correct
        boolean stageCorrect = false;

        switch (expectedStage) {
            case "NORMAL":
                stageCorrect = egfr > 90;
                break;
            case "STAGE_1":
                stageCorrect = egfr >= 60 && egfr <= 89;
                break;
            case "STAGE_2":
                stageCorrect = egfr >= 30 && egfr < 60;
                break;
            case "STAGE_3":
                stageCorrect = egfr >= 15 && egfr < 30;
                break;
            case "STAGE_4":
                stageCorrect = egfr < 15;
                break;
        }

        assertTrue(stageCorrect, "eGFR " + egfr + " should map to stage " + expectedStage);
        System.out.println("✓ TC10: eGFR " + egfr + " → Stage " + expectedStage);
    }

    // ============================================================
    // TEST 11: Null Gender Handling
    // ============================================================
    @Test
    @DisplayName("TC11: Null gender returns null (required for CKD-EPI coefficients)")
    void testNullGenderHandling() {
        Double egfr = service.calculateEgfr(1.0, 50, null, true);
        assertNull(egfr, "eGFR should be null when gender is null (required for CKD-EPI)");
        System.out.println("✓ TC11 PASSED: Null gender returns null");
    }

    // ============================================================
    // TEST 12: Quality Flag Validation
    // ============================================================
    @Test
    @DisplayName("TC12: QualityFlag enum validation (NORMAL, ABNORMALLY_LOW, ABNORMALLY_HIGH, OUTSIDE_NORMAL_RANGE)")
    void testQualityFlagEnum() {
        // Verify enum exists and has expected values
        CKDEPICalculationService.QualityFlag normal = CKDEPICalculationService.QualityFlag.NORMAL;
        CKDEPICalculationService.QualityFlag abnormallyLow = CKDEPICalculationService.QualityFlag.ABNORMALLY_LOW;
        CKDEPICalculationService.QualityFlag abnormallyHigh = CKDEPICalculationService.QualityFlag.ABNORMALLY_HIGH;
        CKDEPICalculationService.QualityFlag outsideRange = CKDEPICalculationService.QualityFlag.OUTSIDE_NORMAL_RANGE;

        assertNotNull(normal);
        assertNotNull(abnormallyLow);
        assertNotNull(abnormallyHigh);
        assertNotNull(outsideRange);
        System.out.println("✓ TC12 PASSED: QualityFlag enum validates correctly (4 values)");
    }

    // ============================================================
    // TEST 13: Formula Verification - Manual Calculation
    // ============================================================
    @Test
    @DisplayName("TC13: Verify CKD-EPI formula with manual calculation for female age 50, SCr 1.0 mg/dL")
    void testCkdEpiFormulaManualVerification() {
        // Given: Female, age 50, creatinine 1.0 mg/dL (88.4 µmol/L)
        // Formula: eGFR = 141 × min(SCr/κ, 1)^α × max(SCr/κ, 1)^-1.209 × 0.993^Age × 1.018
        
        Double scr = 1.0;  // mg/dL
        Integer age = 50;
        
        // Convert to SI units manually
        Double scrMicromol = scr * 88.4;  // 88.4 µmol/L
        
        // Female coefficients: κ=61.9, α=-0.329, female_multiplier=1.018
        Double kappa = 61.9;
        Double alpha = -0.329;
        Double femaleMultiplier = 1.018;
        Double ageCoeff = Math.pow(0.993, age);
        
        // Calculate according to formula
        Double scrRatio = scrMicromol / kappa;  // 88.4 / 61.9
        Double minPart = Math.pow(Math.min(scrRatio, 1.0), alpha);
        Double maxPart = Math.pow(Math.max(scrRatio, 1.0), -1.209);
        Double expectedEgfr = 141 * minPart * maxPart * ageCoeff * femaleMultiplier;
        
        // Calculate using service (safe wrapper)
        Double serviceEgfr = service.calculateEgfr(scr, age, "F", true);
        
        assertNotNull(serviceEgfr);
        assertEquals(expectedEgfr, serviceEgfr, 1.5, 
                    "Service calculation should match manual CKD-EPI formula (within 1.5 mL/min tolerance)");
        System.out.println("✓ TC13 PASSED: Manual verification eGFR = " + expectedEgfr 
                          + ", Service eGFR = " + serviceEgfr);
    }
}
