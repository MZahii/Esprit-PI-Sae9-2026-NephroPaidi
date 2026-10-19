package tn.esprit.spring.clinicalservice.consultation.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LabUnitConversionService — créatinine mg/dL ↔ µmol/L")
class LabUnitConversionServiceTest {

    private final LabUnitConversionService service = new LabUnitConversionService();

    @Test
    void convertitDansLesDeuxSens() {
        assertEquals(88.4, service.convertMgdlToMicromolPerL(1.0), 0.001);
        assertEquals(1.0, service.convertMicromolPerLToMgdl(88.4), 0.001);
        assertEquals(26.5, service.convertMgdlToMicromolPerL(0.3), 0.001); // hors plage typique : warning seulement
        assertEquals(22.62, service.convertMicromolPerLToMgdl(2000), 0.01);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1})
    void refuseUneValeurNulleOuNegative(double v) {
        assertThrows(IllegalArgumentException.class, () -> service.convertMgdlToMicromolPerL(v));
        assertThrows(IllegalArgumentException.class, () -> service.convertMicromolPerLToMgdl(v));
    }

    @Test
    void detecteAutomatiquementLUnite() {
        assertEquals(53.0, service.convertToMicromolPerL(0.6, null), 0.1);
        assertEquals(53.0, service.convertToMicromolPerL(0.6, ""), 0.1);
        assertEquals(60.0, service.convertToMicromolPerL(60.0, null), 0.001);
        assertEquals("MG_DL", service.detectUnit(1.2));
        assertEquals("MICROMOL_L", service.detectUnit(90));
    }

    @Test
    void respecteUneUniteExplicite() {
        assertEquals(88.4, service.convertToMicromolPerL(1.0, "mg/dL"), 0.001);
        assertEquals(88.4, service.convertToMicromolPerL(1.0, "MGDL"), 0.001);
        assertEquals(70.0, service.convertToMicromolPerL(70.0, "umol/L"), 0.001);
        assertEquals(70.0, service.convertToMicromolPerL(70.0, "MICROMOL_L"), 0.001);
        assertThrows(IllegalArgumentException.class, () -> service.convertToMicromolPerL(1.0, "g/L"));
    }

    @Test
    void affichageEtTrace() {
        assertEquals("mg/dL", service.getUnitDisplay("mg_dl"));
        assertEquals("µmol/L", service.getUnitDisplay("MICROMOL_L"));
        assertEquals("other", service.getUnitDisplay("other"));

        LabUnitConversionService.ConversionRecord rec = service.recordConversion(1.0, "MG_DL", 88.4, "MICROMOL_L");
        assertEquals(88.4, rec.getConversionFactor(), 0.001);
        assertTrue(rec.toString().contains("MG_DL"));
        assertEquals(0, service.recordConversion(0, "MG_DL", 0, "MICROMOL_L").getConversionFactor());
    }
}
