package tn.esprit.spring.opsservice.dossier.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tn.esprit.spring.opsservice.dossier.model.SignatureType;

import java.time.LocalDateTime;

public class SignDossierEntryRequest {

    @NotNull
    private SignatureType signatureType;

    @NotBlank
    private String signaturePayload;

    private LocalDateTime signedAt;

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

    public LocalDateTime getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(LocalDateTime signedAt) {
        this.signedAt = signedAt;
    }
}
