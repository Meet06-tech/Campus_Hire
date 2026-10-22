package com.placementmanagement.api.auth;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.service.ProfilePhotoStorageService;

class ProfilePhotoStorageServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void storesOnlyValidatedPngContentWithGeneratedKey() throws Exception {
        byte[] png = imageBytes("png");
        ProfilePhotoStorageService storage = new ProfilePhotoStorageService(tempDir.toString(), 5 * 1024 * 1024);

        String key = storage.store(new MockMultipartFile("file", "unsafe.exe", "application/octet-stream", png));

        assertEquals(true, key.endsWith(".png"));
        assertArrayEquals(png, storage.read(key));
        assertEquals(true, Files.exists(tempDir.resolve(key)));
    }

    @Test
    void rejectsMalformedAndOversizedContent() {
        ProfilePhotoStorageService storage = new ProfilePhotoStorageService(tempDir.toString(), 10);

        ResponseStatusException malformed = assertThrows(ResponseStatusException.class,
                () -> storage.store(new MockMultipartFile("file", "photo.png", "image/png", new byte[] {1, 2, 3})));
        assertEquals(400, malformed.getStatusCode().value());

        ResponseStatusException oversized = assertThrows(ResponseStatusException.class,
                () -> storage.store(new MockMultipartFile("file", "photo.png", "image/png", new byte[11])));
        assertEquals(413, oversized.getStatusCode().value());
    }

    @Test
    void rejectsUnsafeStorageKeys() {
        ProfilePhotoStorageService storage = new ProfilePhotoStorageService(tempDir.toString(), 5 * 1024 * 1024);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> storage.read("../outside.png"));

        assertEquals(404, exception.getStatusCode().value());
    }

    private byte[] imageBytes(String format) throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
