package com.placementmanagement.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.placementmanagement.api.PlacementManagementApplication;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.placement.dto.PlacementDtos.CompanyRequest;
import com.placementmanagement.api.placement.entity.Company;
import com.placementmanagement.api.placement.repository.CompanyRepository;
import com.placementmanagement.api.placement.service.PlacementAuthorizationService;
import com.placementmanagement.api.placement.service.PlacementService;

import jakarta.persistence.EntityManager;

@SpringBootTest(classes = PlacementManagementApplication.class)
@ActiveProfiles("integration-test")
@Import(IntegrationDatabaseGuard.class)
class CompanyEditPostgresIntegrationTest {
    @Autowired CompanyRepository companies;
    @Autowired PlacementService placement;
    @Autowired EntityManager entityManager;
    @MockitoBean PlacementAuthorizationService authorization;

    @Test
    @Transactional
    void editedCoreFieldsSurviveDatabaseReloadWithoutCreatingAnotherCompany() {
        Company existing = companies.saveAndFlush(new Company("ABC Technologies", "Old profile", null, null, null, null));
        Long id = existing.getId();
        entityManager.clear();
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authorization.staff(authentication)).thenReturn(org.mockito.Mockito.mock(UserAccount.class));

        var response = placement.updateCompany(authentication, id,
                new CompanyRequest("XYZ Technologies", "New profile", "https://example.org",
                        "HR Name", "hr@example.org", "12345"));
        entityManager.flush();
        entityManager.clear();

        assertEquals(id, response.id());
        Company reloaded = companies.findById(id).orElseThrow();
        assertEquals("XYZ Technologies", reloaded.getName());
        assertEquals("New profile", reloaded.getDescription());
        assertEquals("https://example.org", reloaded.getWebsite());
        assertEquals("HR Name", reloaded.getHrContactName());
        assertEquals("hr@example.org", reloaded.getHrContactEmail());
        assertEquals("12345", reloaded.getHrContactPhone());
    }
}
