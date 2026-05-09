package tn.esprit.spring.opsservice.dossier.dto.response;

import tn.esprit.spring.opsservice.dossier.model.SignatureType;

import java.time.LocalDateTime;
import java.util.UUID;

public class DossierEntrySignatureResponse {

    private UUID entryId;
    private boolean signed;
    private LocalDateTime signedAt;
    private SignatureType signatureType;

    public UUID getEntryId() {
        return entryId;
    }

    public void setEntryId(UUID entryId) {
        this.entryId = entryId;
    }

    public boolean isSigned() {
        return signed;
    }

    public void setSigned(boolean signed) {
        this.signed = signed;
    }

    public LocalDateTime getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(LocalDateTime signedAt) {
        this.signedAt = signedAt;
    }

    public SignatureType getSignatureType() {
        return signatureType;
    }

    public void setSignatureType(SignatureType signatureType) {
        this.signatureType = signatureType;
    }
}
