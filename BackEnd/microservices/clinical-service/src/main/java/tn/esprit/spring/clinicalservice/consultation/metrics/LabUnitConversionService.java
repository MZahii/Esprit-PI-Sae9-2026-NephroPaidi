package tn.esprit.spring.clinicalservice.consultation.metrics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Lab Unit Conversion Service - Handles conversion between different measurement units
 * 
 * Supported conversions:
 * - Serum Creatinine: mg/dL ↔ µmol/L
 * 
 * European standard uses SI units (µmol/L)
 * US/older systems use mg/dL
 * 
 * Conversion factor: 1 mg/dL = 88.4 µmol/L
 * 
 * @author Clinical Service Team
 */
@Slf4j
@Service
public class LabUnitConversionService {

    // Conversion factors
    private static final double MGDL_TO_MICROMOL_FACTOR = 88.4;
    private static final double MICROMOL_TO_MGDL_FACTOR = 1.0 / 88.4;

    /**
     * Convert serum creatinine from mg/dL to µmol/L
     * 
     * @param valueInMgdl Serum creatinine value in mg/dL
     * @return Value in µmol/L, rounded to 1 decimal place
     * @throws IllegalArgumentException if value is invalid
     */
    public double convertMgdlToMicromolPerL(double valueInMgdl) {
        validateCreatinineValue(valueInMgdl, "mg/dL");
        
        double result = valueInMgdl * MGDL_TO_MICROMOL_FACTOR;
        result = Math.round(result * 10.0) / 10.0;
        
        log.debug("Converted {} mg/dL to {} µmol/L", valueInMgdl, result);
        
        return result;
    }

    /**
     * Convert serum creatinine from µmol/L to mg/dL
     * 
     * @param valueInMicromolPerL Serum creatinine value in µmol/L
     * @return Value in mg/dL, rounded to 2 decimal places
     * @throws IllegalArgumentException if value is invalid
     */
    public double convertMicromolPerLToMgdl(double valueInMicromolPerL) {
        validateCreatinineValue(valueInMicromolPerL, "µmol/L");
        
        double result = valueInMicromolPerL * MICROMOL_TO_MGDL_FACTOR;
        result = Math.round(result * 100.0) / 100.0;
        
        log.debug("Converted {} µmol/L to {} mg/dL", valueInMicromolPerL, result);
        
        return result;
    }

    /**
     * Auto-detect and convert serum creatinine to µmol/L (SI standard)
     * 
     * If value appears to be in mg/dL (< 30), converts to µmol/L
     * If value appears to be in µmol/L (> 30), returns as-is
     * 
     * @param value Serum creatinine value (unit auto-detected)
     * @param explicitUnit Optional explicit unit: "MG_DL", "MGDL", "mg/dL", "MICROMOL_L", "µmol/L", etc.
     * @return Value in µmol/L
     */
    public double convertToMicromolPerL(double value, String explicitUnit) {
        if (explicitUnit != null && !explicitUnit.isEmpty()) {
            return convertWithExplicitUnit(value, explicitUnit);
        }

        // Auto-detect based on value
        if (value < 30) {
            log.info("Auto-detected value {} as mg/dL. Converting to µmol/L.", value);
            return convertMgdlToMicromolPerL(value);
        } else {
            log.info("Auto-detected value {} as µmol/L. Using as-is.", value);
            return value;
        }
    }

    /**
     * Convert with explicit unit specification
     */
    private double convertWithExplicitUnit(double value, String unit) {
        String normalizedUnit = unit.trim().toUpperCase().replace("/", "_").replace(".", "");

        boolean isMgdl = normalizedUnit.equals("MG_DL") || 
                         normalizedUnit.equals("MGDL") ||
                         normalizedUnit.equals("MGDL_") ||
                         normalizedUnit.equals("MG_DL_");

        boolean isMicromol = normalizedUnit.equals("MICROMOL_L") ||
                            normalizedUnit.equals("MICROMOLL") ||
                            normalizedUnit.equals("ΜMOL_L") ||
                            normalizedUnit.equals("UMOL_L");

        if (isMgdl) {
            return convertMgdlToMicromolPerL(value);
        } else if (isMicromol) {
            validateCreatinineValue(value, "µmol/L");
            return value;
        } else {
            throw new IllegalArgumentException(
                    String.format("Unknown creatinine unit: '%s'. Expected: 'MG_DL' or 'MICROMOL_L'", unit));
        }
    }

    /**
     * Validate serum creatinine value (in any unit)
     */
    private void validateCreatinineValue(double value, String unit) {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    String.format("Serum creatinine must be > 0 %s, got %f", unit, value));
        }

        // Check for reasonable ranges
        if ("mg/dL".equals(unit) || "mgdl".equals(unit.toLowerCase())) {
            if (value < 0.3 || value > 22.6) {  // 0.3 mg/dL ≈ 26 µmol/L, 22.6 mg/dL ≈ 2000 µmol/L
                log.warn("Serum creatinine {} {} is outside typical range (0.4-1.3 mg/dL)", value, unit);
            }
        } else if ("µmol/L".equals(unit) || "micromol/l".equals(unit.toLowerCase())) {
            if (value < 30 || value > 2000) {
                log.warn("Serum creatinine {} {} is outside typical range (60-110 µmol/L)", value, unit);
            }
        }
    }

    /**
     * Determine standard unit for a given value (heuristic)
     * 
     * @param value Serum creatinine value
     * @return "MG_DL" or "MICROMOL_L"
     */
    public String detectUnit(double value) {
        if (value < 30) {
            return "MG_DL";
        } else {
            return "MICROMOL_L";
        }
    }

    /**
     * Get human-readable unit string
     */
    public String getUnitDisplay(String unitCode) {
        return switch (unitCode.toUpperCase()) {
            case "MG_DL", "MGDL" -> "mg/dL";
            case "MICROMOL_L", "ΜMOL_L" -> "µmol/L";
            default -> unitCode;
        };
    }

    /**
     * Create conversion reference info for audit/logging
     */
    public ConversionRecord recordConversion(double originalValue, String originalUnit, 
                                             double convertedValue, String convertedUnit) {
        return ConversionRecord.builder()
                .originalValue(originalValue)
                .originalUnit(originalUnit)
                .convertedValue(convertedValue)
                .convertedUnit(convertedUnit)
                .conversionFactor(originalValue > 0 ? convertedValue / originalValue : 0)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // ============================================================================
    // Data Classes
    // ============================================================================

    @lombok.Builder
    @lombok.Getter
    public static class ConversionRecord {
        private double originalValue;
        private String originalUnit;
        private double convertedValue;
        private String convertedUnit;
        private double conversionFactor;
        private long timestamp;

        @Override
        public String toString() {
            return String.format("%f %s → %f %s (factor: %.4f)", 
                    originalValue, originalUnit, convertedValue, convertedUnit, conversionFactor);
        }
    }
}
