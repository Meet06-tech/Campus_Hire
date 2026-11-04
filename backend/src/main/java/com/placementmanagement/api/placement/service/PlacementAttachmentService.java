package com.placementmanagement.api.placement.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.placement.dto.PlacementDtos.AttachmentResponse;
import com.placementmanagement.api.placement.entity.*;
import com.placementmanagement.api.placement.repository.*;

@Service
public class PlacementAttachmentService {
    private final PlacementDriveRepository drives;
    private final PlacementDriveAttachmentRepository attachments;
    private final PlacementAuthorizationService authorization;
    private final PlacementAttachmentStorageService storage;
    public PlacementAttachmentService(PlacementDriveRepository drives, PlacementDriveAttachmentRepository attachments,
            PlacementAuthorizationService authorization, PlacementAttachmentStorageService storage) {
        this.drives = drives; this.attachments = attachments; this.authorization = authorization; this.storage = storage;
    }
    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(Authentication auth, Long driveId) {
        access(auth, driveId);
        return attachments.findAllByDrive_IdOrderByCreatedAtAsc(driveId).stream().map(this::response).toList();
    }
    @Transactional
    public AttachmentResponse upload(Authentication auth, Long driveId, MultipartFile file) {
        PlacementDrive drive = access(auth, driveId);
        if (authorization.staff(auth).getRole() == UserRole.DTPO && drive.getStatus() == DriveStatus.COMPLETED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed drives cannot be changed");
        String key = storage.store(file);
        try { return response(attachments.save(new PlacementDriveAttachment(drive, safeName(file), key, contentType(file), file.getSize()))); }
        catch (RuntimeException ex) { throw ex; }
    }
    public StoredAttachment download(Authentication auth, Long attachmentId) {
        PlacementDriveAttachment attachment = attachments.findById(attachmentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
        access(auth, attachment.getDrive().getId());
        return new StoredAttachment(attachment.getOriginalFileName(), attachment.getContentType(), storage.read(attachment.getStorageKey()));
    }
    private PlacementDrive access(Authentication auth, Long driveId) {
        PlacementDrive drive = drives.findById(driveId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Placement drive not found"));
        try {
            var user = authorization.staff(auth);
            if (user.getRole() == UserRole.DTPO && (drive.getDepartment() == null || user.getDepartment() == null || !user.getDepartment().getId().equals(drive.getDepartment().getId())))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Drive is outside your department");
        } catch (ResponseStatusException staffDenied) {
            var student = authorization.student(auth);
            if (drive.getStatus() != DriveStatus.PUBLISHED
                    || (drive.getDepartment() != null && (student.getDepartment() == null || !drive.getDepartment().getId().equals(student.getDepartment().getId()))))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Attachment is not available");
        }
        return drive;
    }
    private AttachmentResponse response(PlacementDriveAttachment a) { return new AttachmentResponse(a.getId(), a.getOriginalFileName(), a.getContentType(), a.getSizeBytes()); }
    private String safeName(MultipartFile file) { return file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename().replaceAll("[^A-Za-z0-9._ -]", "_"); }
    private String contentType(MultipartFile file) { return file.getContentType() == null ? "application/octet-stream" : file.getContentType(); }
    public record StoredAttachment(String fileName, String contentType, byte[] bytes) {}
}
