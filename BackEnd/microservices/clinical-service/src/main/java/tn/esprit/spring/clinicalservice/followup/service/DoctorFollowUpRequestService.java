package tn.esprit.spring.clinicalservice.followup.service;

import tn.esprit.spring.clinicalservice.followup.dto.*;

import java.util.List;
import java.util.UUID;

public interface DoctorFollowUpRequestService {

    DoctorFollowUpResponse createFromConsultation(UUID consultationId, UUID doctorId, DoctorFollowUpCreateRequest request);

    List<DoctorFollowUpResponse> listPending();

    DoctorFollowUpResponse confirm(UUID requestId, DoctorFollowUpConfirmRequest request);
}
