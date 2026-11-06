package com.placementmanagement.api.placement.controller;

import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.placementmanagement.api.placement.dto.PlacementDtos.AttachmentResponse;
import com.placementmanagement.api.placement.service.PlacementAttachmentService;
import com.placementmanagement.api.placement.service.PlacementAttachmentService.StoredAttachment;

@RestController
@RequestMapping("/api/placement/drives/{driveId}/attachments")
public class PlacementAttachmentController {
    private final PlacementAttachmentService service;
    public PlacementAttachmentController(PlacementAttachmentService service) { this.service = service; }
    @GetMapping public List<AttachmentResponse> list(@PathVariable Long driveId, Authentication auth) { return service.list(auth, driveId); }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse upload(@PathVariable Long driveId, @RequestPart("file") MultipartFile file, Authentication auth) { return service.upload(auth, driveId, file); }
    @GetMapping("/{attachmentId}")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long driveId, @PathVariable Long attachmentId, Authentication auth) {
        StoredAttachment file = service.download(auth, attachmentId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType())).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"").body(new ByteArrayResource(file.bytes()));
    }
}
