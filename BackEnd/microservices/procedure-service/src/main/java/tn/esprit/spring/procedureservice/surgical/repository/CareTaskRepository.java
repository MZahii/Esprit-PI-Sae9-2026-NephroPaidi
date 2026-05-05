package tn.esprit.spring.procedureservice.surgical.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.spring.procedureservice.surgical.domain.entity.CareTask;

public interface CareTaskRepository extends JpaRepository<CareTask, Long> {
    @Query("SELECT CASE WHEN COUNT(ct) > 0 THEN true ELSE false END FROM CareTask ct WHERE ct.surgicalCase.id = :surgicalCaseId AND ct.title = :title")
    boolean existsBySurgicalCaseIdAndTitle(@Param("surgicalCaseId") Long surgicalCaseId, @Param("title") String title);

    List<CareTask> findBySurgicalCaseIdOrderByIdAsc(Long surgicalCaseId);
}
