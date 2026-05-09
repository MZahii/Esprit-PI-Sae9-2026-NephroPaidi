package tn.esprit.spring.clinicalservice.consultation.metrics;

public enum CkdStage {
    G1,
    G2,
    G3A,
    G3B,
    G4,
    G5;

    public static CkdStage fromEgfr(double egfr) {
        if (egfr >= 90) {
            return G1;
        }
        if (egfr >= 60) {
            return G2;
        }
        if (egfr >= 45) {
            return G3A;
        }
        if (egfr >= 30) {
            return G3B;
        }
        if (egfr >= 15) {
            return G4;
        }
        return G5;
    }
}
