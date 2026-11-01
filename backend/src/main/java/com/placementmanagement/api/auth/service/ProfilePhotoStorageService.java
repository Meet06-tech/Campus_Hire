package com.placementmanagement.api.auth.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfilePhotoStorageService {
    private static final long MAX_SIZE_BYTES = 5L * 1024L * 1024L;

    private final Path root;
    private final long maxSizeBytes;

    public ProfilePhotoStorageService(
            @Value("${app.profile-photo.storage-dir:./var/uploads/profile-photos}") String storageDirectory,
            @Value("${app.profile-photo.max-size-bytes:5242880}") long maxSizeBytes) {
        this.root = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.maxSizeBytes = maxSizeBytes > 0 ? Math.min(maxSizeBytes, MAX_SIZE_BYTES) : MAX_SIZE_BYTES;
    }

    public String store(MultipartFile file) {
        validate(file);
        String extension = detectExtension(file);
        String key = UUID.randomUUID() + extension;
        Path target = resolveKey(key);
        try {
            Files.createDirectories(root);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target);
            }
            return key;
        } catch (IOException exception) {
            deleteQuietly(key);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Profile photo could not be stored");
        }
    }

    public byte[] read(String key) {
        Path path = resolveKey(key);
        try {
            return Files.readAllBytes(path);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile photo not found");
        }
    }

    public void deleteQuietly(String key) {
        if (key == null || key.isBlank()) return;
        try {
            Files.deleteIfExists(resolveKey(key));
        } catch (IOException ignored) {
            // Orphan cleanup must not turn a successful database update into an API failure.
        }
    }

    public String contentType(String key) {
        if (key != null && key.toLowerCase(Locale.ROOT).endsWith(".png")) {
            return "image/png";
        }
        return "image/jpeg";
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An image file is required");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Profile photo exceeds the 5 MB limit");
        }
    }

    private String detectExtension(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            var image = ImageIO.read(input);
            if (image == null) {
                throw unsupportedImage();
            }
            if (isPng(file)) return ".png";
            if (isJpeg(file)) return ".jpg";
            throw unsupportedImage();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The image could not be read");
        }
    }

    private boolean isPng(MultipartFile file) throws IOException {
        try (InputStream input = file.getInputStream()) {
            byte[] signature = input.readNBytes(8);
            return signature.length == 8
                    && signature[0] == (byte) 0x89 && signature[1] == 0x50
                    && signature[2] == 0x4E && signature[3] == 0x47
                    && signature[4] == 0x0D && signature[5] == 0x0A
                    && signature[6] == 0x1A && signature[7] == 0x0A;
        }
    }

    private boolean isJpeg(MultipartFile file) throws IOException {
        try (InputStream input = file.getInputStream()) {
            byte[] signature = input.readNBytes(3);
            return signature.length == 3
                    && (signature[0] & 0xFF) == 0xFF
                    && (signature[1] & 0xFF) == 0xD8
                    && (signature[2] & 0xFF) == 0xFF;
        }
    }

    private ResponseStatusException unsupportedImage() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only valid JPEG or PNG images are supported");
    }

    private Path resolveKey(String key) {
        if (!StringUtils.hasText(key) || key.contains("/") || key.contains("\\")
                || !key.matches("^[A-Za-z0-9-]+\\.(jpg|png)$")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile photo not found");
        }
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile photo not found");
        }
        return resolved;
    }
}
