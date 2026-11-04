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
    public CompanyAttachmentService(CompanyRepository companies, CompanyAttachmentRepository attachments,
            PlacementAuthorizationService authorization, PlacementAttachmentStorageService storage) {
        this.companies = companies; this.attachments = attachments; this.authorization = authorization; this.storage = storage;
    }
    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(Authentication auth, Long companyId) {
        authorization.staff(auth); requireCompany(companyId);
        return attachments.findAllByCompany_IdOrderByCreatedAtAsc(companyId).stream().map(this::response).toList();
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
    private AttachmentResponse response(CompanyAttachment a) { return new AttachmentResponse(a.getId(), a.getOriginalFileName(), a.getContentType(), a.getSizeBytes()); }
    private String safeName(MultipartFile file) { return file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename().replaceAll("[^A-Za-z0-9._ -]", "_"); }
    private String contentType(MultipartFile file) { return file.getContentType() == null ? "application/octet-stream" : file.getContentType(); }
}
