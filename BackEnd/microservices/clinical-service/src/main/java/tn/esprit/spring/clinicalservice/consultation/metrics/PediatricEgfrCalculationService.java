package tn.esprit.spring.clinicalservice.consultation.metrics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Pediatric eGFR support using the bedside Schwartz equation.
 *
 * Formula:
 * eGFR = 0.413 x height(cm) / serum creatinine(mg/dL)
 */
@Slf4j
@Service
public class PediatricEgfrCalculationService {

    private static final double SCHWARTZ_FACTOR = 0.413;

    public Double calculateEgfr(Double heightCm, Double creatinineMgDl, Integer ageYears) {
        if (heightCm == null || heightCm <= 0 || creatinineMgDl == null || creatinineMgDl <= 0) {
            return null;
        }
        if (ageYears != null && ageYears >= 18) {
            log.debug("Skipping pediatric Schwartz calculation because age {} is adult-range", ageYears);
            return null;
        }

        double value = (SCHWARTZ_FACTOR * heightCm) / creatinineMgDl;
        return Math.round(value * 10.0) / 10.0;
    }
}
