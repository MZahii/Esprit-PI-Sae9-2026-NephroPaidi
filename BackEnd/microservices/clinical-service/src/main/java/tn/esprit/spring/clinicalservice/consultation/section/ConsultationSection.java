package tn.esprit.spring.clinicalservice.consultation.section;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "consultation_section")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationSection {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "consultation_id", nullable = false)
    private UUID consultationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false, length = 32)
    private ConsultationSectionType sectionType;

    @Column(name = "checked", nullable = false)
    private boolean checked;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
