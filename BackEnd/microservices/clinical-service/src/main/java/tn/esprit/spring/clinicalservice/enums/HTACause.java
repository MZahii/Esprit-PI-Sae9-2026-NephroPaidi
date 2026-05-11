package tn.esprit.spring.clinicalservice.enums;

/**
 * Secondary causes of hypertension in children
 */
public enum HTACause {
    RENAL_PARENCHYMAL("Renal Parenchymal Disease (CKD, glomerulonephritis, reflux nephropathy)"),
    RENAL_ARTERY_STENOSIS("Renal Artery Stenosis (fibromuscular dysplasia, atherosclerotic)"),
    RENAL_TUMOR("Renal Tumor (Wilms, hemangioblastoma)"),
    COARCTATION_AORTA("Coarctation of the Aorta"),
    PHEOCHROMOCYTOMA("Pheochromocytoma (catecholamine-secreting tumor)"),
    NEUROBLASTOMA("Neuroblastoma (catecholamine excess)"),
    ADRENAL_HYPERPLASIA("Adrenal Hyperplasia (aldosterone or cortisol excess)"),
    CUSHING("Cushing Syndrome (glucocorticoid excess)"),
    ESSENTIAL("Essential Hypertension");

    private final String description;

    HTACause(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
