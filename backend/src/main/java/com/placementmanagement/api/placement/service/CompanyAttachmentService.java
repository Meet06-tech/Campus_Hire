package com.placementmanagement.api.placement.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.placementmanagement.api.placement.dto.PlacementDtos.AttachmentResponse;
import com.placementmanagement.api.placement.entity.CompanyAttachment;
import com.placementmanagement.api.placement.repository.CompanyAttachmentRepository;
import com.placementmanagement.api.placement.repository.CompanyRepository;

@Service
public class CompanyAttachmentService {
    private final CompanyRepository companies;
    private final CompanyAttachmentRepository attachments;
    private final PlacementAuthorizationService authorization;
    private final PlacementAttachmentStorageService storage;
    private final PlacementService placement;
    public CompanyAttachmentService(CompanyRepository companies, CompanyAttachmentRepository attachments,
            PlacementAuthorizationService authorization, PlacementAttachmentStorageService storage, PlacementService placement) {
        this.companies = companies; this.attachments = attachments; this.authorization = authorization; this.storage = storage;
        this.placement = placement;
    }
    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(Authentication auth, Long companyId) {
        requireViewAccess(auth, companyId); requireCompany(companyId);
        return attachments.findAllByCompany_IdOrderByCreatedAtAsc(companyId).stream().map(this::response).toList();
    }
    @Transactional(readOnly = true)
    public StoredAttachment download(Authentication auth, Long companyId, Long attachmentId) {
        requireViewAccess(auth, companyId);
        requireCompany(companyId);
        CompanyAttachment attachment = attachments.findById(attachmentId)
                .filter(item -> item.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
        return new StoredAttachment(attachment.getOriginalFileName(), attachment.getContentType(),
                storage.read(attachment.getStorageKey()));
    }
    @Transactional
    public AttachmentResponse upload(Authentication auth, Long companyId, MultipartFile file) {
        authorization.staff(auth);
        var company = requireCompany(companyId);
        String key = storage.store(file);
        return response(attachments.save(new CompanyAttachment(company, safeName(file), key, contentType(file), file.getSize())));
    }
    private com.placementmanagement.api.placement.entity.Company requireCompany(Long id) {
        return companies.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }
    private void requireViewAccess(Authentication auth, Long companyId) {
        try {
            authorization.staff(auth);
            return;
        } catch (ResponseStatusException denied) {
            if (denied.getStatusCode() != HttpStatus.FORBIDDEN) throw denied;
        }
        authorization.student(auth);
        if (placement.listStudentDrives(auth).stream().noneMatch(drive -> drive.company().id().equals(companyId)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Company attachments are not available");
    }
    public record StoredAttachment(String fileName, String contentType, byte[] bytes) {}
    private AttachmentResponse response(CompanyAttachment a) { return new AttachmentResponse(a.getId(), a.getOriginalFileName(), a.getContentType(), a.getSizeBytes()); }
    private String safeName(MultipartFile file) { return file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename().replaceAll("[^A-Za-z0-9._ -]", "_"); }
    private String contentType(MultipartFile file) { return file.getContentType() == null ? "application/octet-stream" : file.getContentType(); }
}
