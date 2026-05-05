package tn.esprit.spring.clinicalservice.labRequest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.spring.clinicalservice.labRequest.dto.CreateLabRequestRequest;
import tn.esprit.spring.clinicalservice.labRequest.dto.LabRequestDto;
import tn.esprit.spring.clinicalservice.labRequest.entity.LabResult;
import tn.esprit.spring.clinicalservice.labRequest.service.LabRequestService;
import tn.esprit.spring.clinicalservice.security.DoctorIdResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clinical/lab-requests")
@RequiredArgsConstructor
public class LabRequestController {

    private final LabRequestService labRequestService;
    private final DoctorIdResolver doctorIdResolver;

    @Value("${clinical.lab-storage-dir:data/lab-results}")
    private String labStorageDir;

    @PostMapping
    public ResponseEntity<LabRequestDto> createLabRequest(
            @RequestBody CreateLabRequestRequest request,
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication) {
        UUID resolvedDoctorId = doctorIdResolver.resolve(doctorId, authentication);
        LabRequestDto response = labRequestService.createLabRequest(request, resolvedDoctorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<LabRequestDto>> getMyLabRequests(
            @RequestHeader(value = "X-Doctor-Id", required = false) UUID doctorId,
            Authentication authentication) {
        UUID resolvedDoctorId = doctorIdResolver.resolve(doctorId, authentication);
        List<LabRequestDto> requests = labRequestService.getLabRequestsByDoctor(resolvedDoctorId);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<LabRequestDto>> getPendingLabRequests() {
        List<LabRequestDto> requests = labRequestService.getPendingLabRequests();
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LabRequestDto> getLabRequest(@PathVariable UUID id) {
        LabRequestDto request = labRequestService.getLabRequestById(id);
        return ResponseEntity.ok(request);
    }

    @PostMapping(value = "/{id}/results", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LabRequestDto> uploadLabResult(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            Authentication authentication) {
        // TODO: Resolve userId from authentication if not provided
        LabRequestDto response = labRequestService.uploadLabResult(id, file, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/consultation/{consultationId}/results/latest/download")
    public ResponseEntity<ByteArrayResource> downloadLatestLabResultForConsultation(@PathVariable UUID consultationId) throws IOException {
        LabResult result = labRequestService.getLatestLabResultForConsultation(consultationId);
        Path path = resolveStoredFilePath(result);
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        byte[] bytes = Files.readAllBytes(path);
        String contentType = result.getContentType();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (contentType != null && !contentType.isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(contentType);
            } catch (Exception ignored) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(result.getFileName() != null ? result.getFileName() : path.getFileName().toString())
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(bytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new ByteArrayResource(bytes));
    }

    private Path resolveStoredFilePath(LabResult result) {
        if (result.getFilePath() == null || result.getFilePath().isBlank()) {
            return Paths.get("__missing__");
        }

        Path storedPath = Paths.get(result.getFilePath()).normalize();
        if (Files.exists(storedPath)) {
            return storedPath;
        }

        if (!storedPath.isAbsolute()) {
            Path cwdResolved = Paths.get("").toAbsolutePath().resolve(storedPath).normalize();
            if (Files.exists(cwdResolved)) {
                return cwdResolved;
            }

            Path appResolved = Paths.get("/app").resolve(storedPath).normalize();
            if (Files.exists(appResolved)) {
                return appResolved;
            }
        }

        if (result.getLabRequestId() != null) {
            String fileName = storedPath.getFileName() != null ? storedPath.getFileName().toString() : result.getFileName();
            if (fileName != null && !fileName.isBlank()) {
                Path storageResolved = Paths.get(labStorageDir)
                        .resolve(result.getLabRequestId().toString())
                        .resolve(fileName)
                        .normalize();
                if (Files.exists(storageResolved)) {
                    return storageResolved;
                }

                Path appStorageResolved = Paths.get("/app").resolve(storageResolved).normalize();
                if (Files.exists(appStorageResolved)) {
                    return appStorageResolved;
                }
            }
        }

        return storedPath;
    }
}
