package com.placementmanagement.api.placement.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import com.placementmanagement.api.placement.dto.PlacementDtos.AttachmentResponse;
import com.placementmanagement.api.placement.service.CompanyAttachmentService;

@RestController
@RequestMapping("/api/placement/companies/{companyId}/attachments")
public class CompanyAttachmentController {
    private final CompanyAttachmentService service;
    public CompanyAttachmentController(CompanyAttachmentService service) { this.service = service; }
    @GetMapping public List<AttachmentResponse> list(@PathVariable Long companyId, Authentication auth) { return service.list(auth, companyId); }
    @PostMapping(consumes = "multipart/form-data") @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse upload(@PathVariable Long companyId, @RequestPart("file") MultipartFile file, Authentication auth) { return service.upload(auth, companyId, file); }
}
