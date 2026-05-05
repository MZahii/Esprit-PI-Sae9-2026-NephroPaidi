package tn.esprit.spring.opsservice.dossier.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tn.esprit.spring.opsservice.dossier.model.SignatureType;

import java.time.LocalDateTime;

public class DischargeDossierRequest {

    @NotBlank
    private String dischargeSummary;

    @NotNull
    private LocalDateTime dischargedAt;

    private Boolean requiresSignedDischargeNote;

    @Valid
    private Signature signature;

    public String getDischargeSummary() {
        return dischargeSummary;
    }

    public void setDischargeSummary(String dischargeSummary) {
        this.dischargeSummary = dischargeSummary;
    }

    public LocalDateTime getDischargedAt() {
        return dischargedAt;
    }

    public void setDischargedAt(LocalDateTime dischargedAt) {
        this.dischargedAt = dischargedAt;
    }

    public Boolean getRequiresSignedDischargeNote() {
        return requiresSignedDischargeNote;
    }

    public void setRequiresSignedDischargeNote(Boolean requiresSignedDischargeNote) {
        this.requiresSignedDischargeNote = requiresSignedDischargeNote;
    }

    public Signature getSignature() {
        return signature;
    }

    public void setSignature(Signature signature) {
        this.signature = signature;
    }

    public static class Signature {
        @NotNull
        private SignatureType signatureType;
        @NotBlank
        private String signaturePayload;

        public SignatureType getSignatureType() {
            return signatureType;
        }

        public void setSignatureType(SignatureType signatureType) {
            this.signatureType = signatureType;
        }

        public String getSignaturePayload() {
            return signaturePayload;
        }

        public void setSignaturePayload(String signaturePayload) {
            this.signaturePayload = signaturePayload;
        }
    }
}
