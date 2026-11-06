package com.placementmanagement.api.placement.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlacementAttachmentStorageService {
    private final Path root;
    private final long maxSize;
    public PlacementAttachmentStorageService(@Value("${app.placement-attachment.storage-dir:./var/uploads/placement-attachments}") String root,
            @Value("${app.placement-attachment.max-size-bytes:10485760}") long maxSize) {
        this.root = Path.of(root).toAbsolutePath().normalize(); this.maxSize = Math.min(Math.max(maxSize, 1), 10L * 1024 * 1024);
    }
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attachment file is required");
        if (file.getSize() > maxSize) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Attachment exceeds the 10 MB limit");
        String key = UUID.randomUUID().toString();
        try { Files.createDirectories(root); try (InputStream in = file.getInputStream()) { Files.copy(in, root.resolve(key)); } return key; }
        catch (IOException ex) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Attachment could not be stored"); }
    }
    public byte[] read(String key) {
        if (key == null || !key.matches("^[a-f0-9-]{36}$")) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found");
        try { return Files.readAllBytes(root.resolve(key).normalize()); }
        catch (IOException ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"); }
    }
}
