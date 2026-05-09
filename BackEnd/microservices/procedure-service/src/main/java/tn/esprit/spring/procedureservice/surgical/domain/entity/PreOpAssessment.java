package tn.esprit.spring.procedureservice.surgical.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "preop_assessments")
public class PreOpAssessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "surgical_case_id")
    private SurgicalCase surgicalCase;

    private String notes;

    public PreOpAssessment() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SurgicalCase getSurgicalCase() { return surgicalCase; }
    public void setSurgicalCase(SurgicalCase surgicalCase) { this.surgicalCase = surgicalCase; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
