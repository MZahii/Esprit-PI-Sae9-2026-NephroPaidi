package tn.esprit.spring.opsservice.hospitalization.application;

import tn.esprit.spring.opsservice.hospitalization.api.dto.CreateHospitalizationRequest;
import tn.esprit.spring.opsservice.hospitalization.api.dto.CreateHospitalizationTaskRequest;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationCaseResponse;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationSummaryResponse;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationTaskResponse;
import tn.esprit.spring.opsservice.hospitalization.api.dto.HospitalizationTaskUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface HospitalizationService {

    HospitalizationCaseResponse createHospitalization(CreateHospitalizationRequest request);

    HospitalizationTaskResponse addTask(UUID hospitalizationId, CreateHospitalizationTaskRequest request);

    HospitalizationCaseResponse getHospitalization(UUID hospitalizationId);

    List<HospitalizationSummaryResponse> getActiveHospitalizationsForNurse();

    HospitalizationTaskResponse recordTaskExecution(UUID taskId, HospitalizationTaskUpdateRequest request);
}
