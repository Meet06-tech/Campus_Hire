package com.placementmanagement.api.placement.controller;

import java.util.List;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    @GetMapping("/{attachmentId}")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long companyId, @PathVariable Long attachmentId,
            Authentication auth) {
        var file = service.download(auth, companyId, attachmentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(new ByteArrayResource(file.bytes()));
    }
}
