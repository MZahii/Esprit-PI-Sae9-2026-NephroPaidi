package tn.esprit.spring.clinicalservice.consultation.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tn.esprit.spring.clinicalservice.consultation.metrics.TrendAnalysisService.EgfrTrend;
import tn.esprit.spring.clinicalservice.consultation.metrics.TrendAnalysisService.TrendData;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TrendAnalysisService — tendance du DFG")
class TrendAnalysisServiceTest {

    private final TrendAnalysisService service = new TrendAnalysisService();

    @Test
    void sansValeurPrecedente_baseline() {
        TrendData t = service.analyzeTrend(60, null);
        assertEquals(EgfrTrend.NO_PREVIOUS_DATA, t.getTrendStatus());
        assertFalse(t.isRequiresAlert());
        assertEquals("secondary", t.getCssClass());
        assertEquals(3, t.getSeverityLevel());
        assertNotNull(t.getIcon());
        assertEquals(EgfrTrend.NO_PREVIOUS_DATA, service.analyzeTrend(60, 0.0).getTrendStatus());
    }

    @Test
    void declinRapide_alerteUrgente() {
        TrendData t = service.analyzeTrend(40, 60.0); // -33,3 %
        assertEquals(EgfrTrend.RAPID_DECLINE, t.getTrendStatus());
        assertTrue(t.isAbnormal());
        assertTrue(t.isRequiresAlert());
        assertEquals(-20.0, t.getAbsoluteChange());
        assertEquals(-33.3, t.getPercentChange());
        assertTrue(t.getRecommendation().contains("urgent"));
        assertEquals(5, t.getSeverityLevel());
        assertEquals("danger", t.getCssClass());
    }

    @Test
    void declinModere() {
        TrendData t = service.analyzeTrend(54, 60.0); // -10 %
        assertEquals(EgfrTrend.DECLINING, t.getTrendStatus());
        assertFalse(t.isAbnormal());
        assertTrue(t.isRequiresAlert());
        assertEquals("warning", t.getCssClass());
        assertEquals(4, t.getSeverityLevel());
    }

    @Test
    void stable() {
        TrendData t = service.analyzeTrend(61, 60.0); // +1,7 %
        assertEquals(EgfrTrend.STABLE, t.getTrendStatus());
        assertFalse(t.isRequiresAlert());
        assertEquals("info", t.getCssClass());
    }

    @Test
    void ameliorationRapide_verifierLeLabo() {
        TrendData t = service.analyzeTrend(80, 60.0); // +33 %
        assertEquals(EgfrTrend.RAPID_IMPROVEMENT, t.getTrendStatus());
        assertTrue(t.isAbnormal());
        assertTrue(t.getRecommendation().contains("Verify"));
        assertEquals(4, t.getSeverityLevel());
    }

    /**
     * Défaut détecté par ce test (D-01) : une hausse de +5 % à +20 % devrait
     * donner IMPROVING (cf. Javadoc), mais determineTrend() renvoie STABLE :
     * la branche IMPROVING est inatteignable. Ce test documente le comportement
     * actuel ; il devra être inversé une fois le défaut corrigé.
     */
    @Test
    void defautD01_ameliorationModereeClasseeStable() {
        TrendData t = service.analyzeTrend(66, 60.0); // +10 %
        assertEquals(EgfrTrend.STABLE, t.getTrendStatus());
    }

    @Test
    void estimationDuDelaiAvantStade5() {
        assertNull(service.estimateTimeToESRDMonths(40, null, 6));
        assertNull(service.estimateTimeToESRDMonths(40, 50.0, 0));
        assertNull(service.estimateTimeToESRDMonths(50, 40.0, 6)); // amélioration
        assertEquals(0, service.estimateTimeToESRDMonths(10, 20.0, 3));
    }

    /**
     * Défaut D-02 : pour un DFG de 30 qui perd 1 point/mois, on attend 15 mois
     * avant le stade 5 ; la méthode calcule (15 - 30) / 1 et renvoie -15.
     * Le signe de la formule est inversé.
     */
    @Test
    void defautD02_delaiNegatif() {
        assertEquals(-15, service.estimateTimeToESRDMonths(30, 40.0, 10));
    }

    @Test
    void descriptionsDesTendances() {
        for (EgfrTrend t : EgfrTrend.values()) {
            assertFalse(t.getDescription().isBlank());
        }
    }
}
