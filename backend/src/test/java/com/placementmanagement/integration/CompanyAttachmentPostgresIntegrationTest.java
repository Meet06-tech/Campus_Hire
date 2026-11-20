package com.placementmanagement.integration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.placementmanagement.api.PlacementManagementApplication;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.placement.entity.Company;
import com.placementmanagement.api.placement.repository.CompanyAttachmentRepository;
import com.placementmanagement.api.placement.repository.CompanyRepository;
import com.placementmanagement.api.placement.service.CompanyAttachmentService;
import com.placementmanagement.api.placement.service.PlacementAuthorizationService;

import jakarta.persistence.EntityManager;

@SpringBootTest(classes = PlacementManagementApplication.class)
@ActiveProfiles("integration-test")
@Import(IntegrationDatabaseGuard.class)
@TestPropertySource(properties = "app.placement-attachment.storage-dir=./target/test-placement-attachments")
class CompanyAttachmentPostgresIntegrationTest {
    @Autowired CompanyRepository companies;
    @Autowired CompanyAttachmentRepository attachments;
    @Autowired CompanyAttachmentService service;
    @Autowired EntityManager entityManager;
    @MockitoBean PlacementAuthorizationService authorization;

    @Test
    @Transactional
    void uploadedCompanyFileMetadataAndBytesSurviveReloadWithinTransaction() throws Exception {
        Company company = companies.saveAndFlush(new Company("Attachment test company", null, null, null, null, null));
        Long companyId = company.getId();
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        when(authorization.staff(auth)).thenReturn(org.mockito.Mockito.mock(UserAccount.class));
        byte[] contents = new byte[] {1, 2, 3, 4};
        var upload = new MockMultipartFile("file", "offer.pdf", "application/pdf", contents);
        String storageKey = null;
        try {
            var saved = service.upload(auth, companyId, upload);
            entityManager.flush();
            entityManager.clear();
            storageKey = attachments.findById(saved.id()).orElseThrow().getStorageKey();

            assertEquals(1, service.list(auth, companyId).size());
            assertEquals("offer.pdf", service.list(auth, companyId).get(0).fileName());
            assertArrayEquals(contents, service.download(auth, companyId, saved.id()).bytes());
        } finally {
            if (storageKey != null) {
                Files.deleteIfExists(Path.of("target/test-placement-attachments", storageKey));
            }
        }
    }
}
