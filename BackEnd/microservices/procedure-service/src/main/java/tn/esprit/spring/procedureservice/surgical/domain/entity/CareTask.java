package tn.esprit.spring.procedureservice.surgical.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "care_tasks")
public class CareTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "surgical_case_id")
    private SurgicalCase surgicalCase;

    private String title;
    private boolean done;

    public CareTask() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SurgicalCase getSurgicalCase() { return surgicalCase; }
    public void setSurgicalCase(SurgicalCase surgicalCase) { this.surgicalCase = surgicalCase; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }
}
