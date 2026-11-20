package com.placementmanagement.api.placement;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;

import com.placementmanagement.api.placement.controller.CompanyAttachmentController;
import com.placementmanagement.api.placement.controller.PlacementAttachmentController;
import com.placementmanagement.api.placement.service.CompanyAttachmentService;
import com.placementmanagement.api.placement.service.PlacementAttachmentService;

class AttachmentControllerTest {
    @Test
    void companyDownloadReturnsStoredBytesAndSafeOriginalName() {
        var service = mock(CompanyAttachmentService.class);
        var auth = mock(Authentication.class);
        when(service.download(auth, 4L, 8L)).thenReturn(
                new CompanyAttachmentService.StoredAttachment("offer.pdf", "application/pdf", new byte[] {1, 2}));

        var response = new CompanyAttachmentController(service).download(4L, 8L, auth);

        assertArrayEquals(new byte[] {1, 2}, response.getBody().getByteArray());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("offer.pdf"));
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE).contains("application/pdf"));
    }

    @Test
    void driveDownloadUsesTheDriveAndAttachmentIdentifiersTogether() {
        var service = mock(PlacementAttachmentService.class);
        var auth = mock(Authentication.class);
        when(service.download(auth, 9L, 3L)).thenReturn(
                new PlacementAttachmentService.StoredAttachment("round.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {3, 4}));

        var response = new PlacementAttachmentController(service).download(9L, 3L, auth);

        assertArrayEquals(new byte[] {3, 4}, response.getBody().getByteArray());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("round.xlsx"));
    }
}
