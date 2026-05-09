package tn.esprit.spring.procedureservice.dialysis.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "dialysis_outcomes")
public class DialysisOutcome {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private DialysisSession session;

    private boolean validated;
    private String summary;

    public DialysisOutcome() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DialysisSession getSession() { return session; }
    public void setSession(DialysisSession session) { this.session = session; }
    public boolean isValidated() { return validated; }
    public void setValidated(boolean validated) { this.validated = validated; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
