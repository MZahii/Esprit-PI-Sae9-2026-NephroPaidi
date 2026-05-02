package tn.esprit.spring.Administrationservice.service.impl;

import tn.esprit.spring.Administrationservice.entity.EquipmentCategory;

import java.util.List;
import java.util.Map;

public final class EquipmentSubtypeCatalog {

    public static final String OTHER = "OTHER";

    public static final Map<EquipmentCategory, List<String>> CATALOG = Map.of(
            EquipmentCategory.IMAGING_EQUIPMENT,
            List.of(
                    "X_RAY_MACHINE",
                    "MOBILE_X_RAY",
                    "ULTRASOUND_MACHINE",
                    "DOPPLER_ULTRASOUND",
                    "CT_SCANNER",
                    "MRI_SCANNER",
                    "FLUOROSCOPY_UNIT",
                    "C_ARM",
                    "MAMMOGRAPHY_UNIT",
                    "DEXA_SCANNER",
                    "PACS_WORKSTATION",
                    "CONTRAST_INJECTOR",
                    OTHER
            ),
            EquipmentCategory.ANALYSIS_EQUIPMENT,
            List.of(
                    "URINE_DIPSTICK_READER",
                    "URINALYSIS_ANALYZER",
                    "URINE_SEDIMENT_MICROSCOPE",
                    "CENTRIFUGE",
                    "REFRIGERATED_CENTRIFUGE",
                    "ELECTROLYTE_ANALYZER",
                    "BLOOD_GAS_ANALYZER",
                    "COAGULATION_ANALYZER",
                    "ELISA_READER",
                    "ELISA_WASHER",
                    "HEMOGLOBIN_METER",
                    "SAMPLE_REFRIGERATOR",
                    "SAMPLE_FREEZER",
                    "INCUBATOR",
                    "WATER_BATH",
                    "PIPETTE_SET",
                    "PCR_ANALYZER",
                    "SPECTROPHOTOMETER",
                    OTHER
            ),
            EquipmentCategory.LABORATORY_EQUIPMENT,
            List.of(
                    "CLINICAL_CHEMISTRY_ANALYZER",
                    "HEMATOLOGY_ANALYZER",
                    "IMMUNOASSAY_ANALYZER",
                    "URINALYSIS_ANALYZER",
                    "ELECTROLYTE_ANALYZER",
                    "BLOOD_GAS_ANALYZER",
                    "COAGULATION_ANALYZER",
                    "HBA1C_ANALYZER",
                    "PCR_MOLECULAR_ANALYZER",
                    "MICROBIOLOGY_INCUBATOR",
                    "BIOSAFETY_CABINET",
                    "OSMOMETER",
                    "LAB_REFRIGERATOR",
                    "LAB_FREEZER",
                    "AUTOCLAVE",
                    OTHER
            )
    );

    private EquipmentSubtypeCatalog() {
    }
}
