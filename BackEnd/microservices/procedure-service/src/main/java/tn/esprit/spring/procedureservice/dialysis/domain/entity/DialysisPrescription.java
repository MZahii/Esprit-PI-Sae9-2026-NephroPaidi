package tn.esprit.spring.procedureservice.dialysis.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "dialysis_prescriptions")
public class DialysisPrescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "plan_id")
    private DialysisPlan plan;

    private String details;

    public DialysisPrescription() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DialysisPlan getPlan() { return plan; }
    public void setPlan(DialysisPlan plan) { this.plan = plan; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
