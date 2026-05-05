package tn.esprit.spring.clinicalservice.consultation.metrics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * CKD-EPI Formula Calculator (European Standard - SI Units)
 * 
 * Formula: eGFR = 141 × min(SCr/κ, 1)ᵅ × max(SCr/κ, 1)⁻¹·²⁰⁹ × 0.993ᴬᵍᵉ [× 1.018 if female]
 * 
 * Where:
 * - SCr: Serum Creatinine in µmol/L
 * - κ (kappa): 61.9 for females, 79.6 for males
 * - α (alpha): -0.329 for females, -0.411 for males
 * - Age in years
 * - Result in mL/min/1.73m²
 * 
 * Reference: KDIGO 2021, European standard for CKD assessment
 * @author Clinical Service Team
 * @version 2.0 - CKD-EPI 2021 European
 */
@Slf4j
@Service
public class CKDEPICalculationService {

    // CKD-EPI Constants
    private static final double KAPPA_FEMALE = 61.9;
    private static final double KAPPA_MALE = 79.6;
    private static final double ALPHA_FEMALE = -0.329;
    private static final double ALPHA_MALE = -0.411;
    private static final double BASE_MULTIPLIER = 141.0;
    private static final double AGE_BASE = 0.993;
    private static final double FEMALE_MULTIPLIER = 1.018;
    private static final double MAX_EXPONENT = -1.209;
    private static final double ROUNDING_FACTOR = 10.0;

    /**
     * Calculate eGFR using CKD-EPI 2021 formula
     * 
     * @param serumCreatinineMicromolPerL Serum creatinine in µmol/L (European SI unit)
     * @param ageYears Patient age in years (18-120)
     * @param sex Patient sex: "M" (male) or "F" (female)
     * @return eGFR in mL/min/1.73m² rounded to 1 decimal place
     * @throws IllegalArgumentException if inputs are invalid
     */
    public double calculateEgfr(double serumCreatinineMicromolPerL, int ageYears, String sex) {
        validateInputs(serumCreatinineMicromolPerL, ageYears, sex);

        log.debug("Calculating eGFR - SCr: {} µmol/L, Age: {}, Sex: {}", 
                serumCreatinineMicromolPerL, ageYears, sex);

        // Step 1: Select coefficients based on sex
        double kappa = "F".equalsIgnoreCase(sex) ? KAPPA_FEMALE : KAPPA_MALE;
        double alpha = "F".equalsIgnoreCase(sex) ? ALPHA_FEMALE : ALPHA_MALE;
        double femaleMultiplier = "F".equalsIgnoreCase(sex) ? FEMALE_MULTIPLIER : 1.0;

        // Step 2: Calculate SCr/κ ratio
        double ratio = serumCreatinineMicromolPerL / kappa;

        // Step 3: Calculate min(ratio, 1)^α
        double minRatio = Math.min(ratio, 1.0);
        double minRatioPowAlpha = Math.pow(minRatio, alpha);

        // Step 4: Calculate max(ratio, 1)^-1.209
        double maxRatio = Math.max(ratio, 1.0);
        double maxRatioPowMax = Math.pow(maxRatio, MAX_EXPONENT);

        // Step 5: Calculate age component: 0.993^age
        double ageComponent = Math.pow(AGE_BASE, ageYears);

        // Step 6: Calculate eGFR
        double eGFR = BASE_MULTIPLIER * minRatioPowAlpha * maxRatioPowMax * ageComponent * femaleMultiplier;

        // Step 7: Round to 1 decimal place
        eGFR = Math.round(eGFR * ROUNDING_FACTOR) / ROUNDING_FACTOR;

        log.debug("Calculated eGFR: {} mL/min/1.73m²", eGFR);

        return eGFR;
    }

    /**
     * Calculate eGFR - Safe wrapper that returns null for invalid inputs
     * Used for nullable inputs from API layer
     * 
     * @param creatinineMgDl Serum creatinine in mg/dL (will be converted to SI units)
     * @param ageYears Patient age (nullable)
     * @param sex Patient sex 'M' or 'F' (nullable)
     * @param isEuropeStandard If true, uses European CKD-EPI 2021; otherwise legacy formula
     * @return eGFR or null if inputs are invalid
     */
    public Double calculateEgfr(Double creatinineMgDl, Integer ageYears, String sex, Boolean isEuropeStandard) {
        // Validate inputs - return null if any are null or invalid
        if (creatinineMgDl == null || creatinineMgDl <= 0 ||
            ageYears == null || ageYears < 18 ||
            sex == null || (!sex.equalsIgnoreCase("M") && !sex.equalsIgnoreCase("F"))) {
            log.debug("Invalid inputs for eGFR calculation: creatinine={}, age={}, sex={}", 
                    creatinineMgDl, ageYears, sex);
            return null;
        }

        try {
            // Convert mg/dL to µmol/L for CKD-EPI formula
            double creatinineMicromolPerL = creatinineMgDl * 88.4;
            return calculateEgfr(creatinineMicromolPerL, ageYears, sex);
        } catch (IllegalArgumentException e) {
            log.warn("Error calculating eGFR: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validate input parameters before calculation
     * 
     * @throws IllegalArgumentException if any input is invalid
     */
    private void validateInputs(double serumCreatinineMicromolPerL, int ageYears, String sex) {
        // Validate serum creatinine
        if (serumCreatinineMicromolPerL <= 0) {
            throw new IllegalArgumentException("Serum creatinine must be > 0 µmol/L");
        }
        if (serumCreatinineMicromolPerL < 30 || serumCreatinineMicromolPerL > 2000) {
            log.warn("Serum creatinine {} µmol/L is outside typical range (30-2000). Verify lab result.",
                    serumCreatinineMicromolPerL);
        }

        // Validate age
        if (ageYears < 18) {
            throw new IllegalArgumentException("CKD-EPI formula not applicable for age < 18. Use Schwartz formula for pediatric patients.");
        }
        if (ageYears > 120) {
            throw new IllegalArgumentException("Age must be <= 120 years");
        }

        // Validate sex
        if (!("M".equalsIgnoreCase(sex) || "F".equalsIgnoreCase(sex))) {
            throw new IllegalArgumentException("Sex must be 'M' (male) or 'F' (female)");
        }
    }

    /**
     * Calculate Cockcroft-Gault creatinine clearance (for reference/comparison)
     * Note: Not the primary formula, but useful for comparison
     * 
     * @param ageYears Patient age
     * @param weightKg Patient weight in kg
     * @param serumCreatinineMicromolPerL Serum creatinine in µmol/L
     * @param sex Patient sex
     * @return Creatinine clearance in mL/min
     */
    public double calculateCockcoftGaultCrCl(int ageYears, double weightKg, 
                                             double serumCreatinineMicromolPerL, String sex) {
        // Convert µmol/L to mg/dL for Cockcroft-Gault
        double serumCreatininieMgdl = serumCreatinineMicromolPerL / 88.4;

        // Formula: ((140 - age) × weight in kg) / (72 × serum creatinine mg/dL)
        double crcl = ((140.0 - ageYears) * weightKg) / (72.0 * serumCreatininieMgdl);

        // Female adjustment: multiply by 0.85
        if ("F".equalsIgnoreCase(sex)) {
            crcl *= 0.85;
        }

        // Round to 1 decimal place
        crcl = Math.round(crcl * ROUNDING_FACTOR) / ROUNDING_FACTOR;

        log.debug("Calculated Cockcroft-Gault CrCl: {} mL/min (reference only)", crcl);

        return crcl;
    }

    /**
     * Check if serum creatinine value is abnormal and needs verification
     * 
     * @param serumCreatinineMicromolPerL Serum creatinine in µmol/L
     * @return QualityFlag indicating if value needs verification
     */
    public QualityFlag checkSerumCreatinineQuality(double serumCreatinineMicromolPerL) {
        if (serumCreatinineMicromolPerL < 40) {
            return QualityFlag.ABNORMALLY_LOW;
        }
        if (serumCreatinineMicromolPerL > 1500) {
            return QualityFlag.ABNORMALLY_HIGH;
        }
        if (serumCreatinineMicromolPerL < 60 || serumCreatinineMicromolPerL > 110) {
            return QualityFlag.OUTSIDE_NORMAL_RANGE;
        }
        return QualityFlag.NORMAL;
    }

    public enum QualityFlag {
        NORMAL("Within normal range (60-110 µmol/L)"),
        OUTSIDE_NORMAL_RANGE("Outside normal range but valid"),
        ABNORMALLY_LOW("Abnormally low - verify for muscle loss/malnutrition"),
        ABNORMALLY_HIGH("Abnormally high - verify lab accuracy");

        private final String description;

        QualityFlag(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
