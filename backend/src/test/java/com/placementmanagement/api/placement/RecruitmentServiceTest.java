package com.placementmanagement.api.placement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.placement.dto.PlacementDtos.RoundRequest;
import com.placementmanagement.api.placement.dto.PlacementDtos.RoundResponse;
import com.placementmanagement.api.placement.entity.PlacementDrive;
import com.placementmanagement.api.placement.entity.RecruitmentRound;
import com.placementmanagement.api.placement.repository.PlacementApplicationRepository;
import com.placementmanagement.api.placement.repository.PlacementDriveRepository;
import com.placementmanagement.api.placement.repository.RecruitmentRoundRepository;
import com.placementmanagement.api.placement.repository.RoundResultRepository;
import com.placementmanagement.api.placement.service.PlacementAuthorizationService;
import com.placementmanagement.api.placement.service.RecruitmentService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecruitmentServiceTest {
    @Mock PlacementDriveRepository drives;
    @Mock PlacementApplicationRepository applications;
    @Mock RecruitmentRoundRepository rounds;
    @Mock RoundResultRepository results;
    @Mock PlacementAuthorizationService authorization;
    @Mock Authentication authentication;

    private RecruitmentService service;
    private PlacementDrive drive;
    private Department department;
    private UserAccount dtpo;

    @BeforeEach
    void setUp() {
        service = new RecruitmentService(drives, applications, rounds, results, authorization);
        drive = mock(PlacementDrive.class);
        department = mock(Department.class);
        dtpo = mock(UserAccount.class);
        when(drive.getId()).thenReturn(7L);
        when(drive.getDepartment()).thenReturn(department);
        when(department.getId()).thenReturn(2L);
        when(dtpo.getRole()).thenReturn(UserRole.DTPO);
        when(dtpo.getDepartment()).thenReturn(department);
        when(authorization.staff(authentication)).thenReturn(dtpo);
        when(drives.findById(7L)).thenReturn(Optional.of(drive));
        when(rounds.findAllByDrive_IdOrderByOrderAsc(7L)).thenReturn(List.of());
        when(rounds.save(any(RecruitmentRound.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(applications.findAllByDrive_IdOrderByAppliedAtAsc(7L)).thenReturn(List.of());
    }

    @Test
    void createsNamedFinalRoundForAuthorizedDepartmentCoordinator() {
        when(rounds.existsByDrive_IdAndFinalRoundTrue(7L)).thenReturn(false);

        RoundResponse response = service.createRound(authentication, 7L,
                new RoundRequest("Technical interview", 2, true));

        assertEquals(2, response.order());
        assertEquals("Technical interview", response.name());
        assertEquals(true, response.finalRound());
    }

    @Test
    void rejectsDriveOutsideCoordinatorDepartment() {
        Department otherDepartment = mock(Department.class);
        when(otherDepartment.getId()).thenReturn(9L);
        when(drive.getDepartment()).thenReturn(otherDepartment);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.listRounds(authentication, 7L));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    void rejectsSecondFinalRound() {
        when(rounds.existsByDrive_IdAndFinalRoundTrue(7L)).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.createRound(authentication, 7L, new RoundRequest("Final", 3, true)));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }
}
