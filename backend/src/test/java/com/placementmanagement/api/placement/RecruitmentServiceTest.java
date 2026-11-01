package com.placementmanagement.api.placement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
import com.placementmanagement.api.auth.entity.Student;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.placement.dto.PlacementDtos.RoundRequest;
import com.placementmanagement.api.placement.dto.PlacementDtos.RoundResponse;
import com.placementmanagement.api.placement.dto.PlacementDtos.FinalOutcomeRequest;
import com.placementmanagement.api.placement.entity.ApplicationStatus;
import com.placementmanagement.api.placement.entity.PlacementDrive;
import com.placementmanagement.api.placement.entity.PlacementApplication;
import com.placementmanagement.api.placement.entity.RecruitmentRound;
import com.placementmanagement.api.placement.entity.RoundResult;
import com.placementmanagement.api.placement.entity.RoundResultStatus;
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

    @Test
    void persistsFinalSelectionAfterPublishedFinalRoundPass() {
        RecruitmentRound finalRound = mock(RecruitmentRound.class);
        PlacementApplication application = mock(PlacementApplication.class);
        RecruitmentRound previousRound = mock(RecruitmentRound.class);
        RoundResult previousResult = mock(RoundResult.class);
        Student student = mock(Student.class);
        when(finalRound.getId()).thenReturn(11L);
        when(finalRound.getOrder()).thenReturn(2);
        when(finalRound.isFinalRound()).thenReturn(true);
        when(finalRound.isResultsPublished()).thenReturn(true);
        when(previousRound.getId()).thenReturn(10L);
        when(previousRound.getOrder()).thenReturn(1);
        when(application.getId()).thenReturn(22L);
        when(application.getDrive()).thenReturn(drive);
        when(application.getStudent()).thenReturn(student);
        when(application.getStatus()).thenReturn(ApplicationStatus.SELECTED);
        when(student.getId()).thenReturn(33L);
        when(previousResult.getResult()).thenReturn(RoundResultStatus.PASSED);
        when(rounds.findAllByDrive_IdOrderByOrderAsc(7L)).thenReturn(List.of(previousRound, finalRound));
        when(applications.findById(22L)).thenReturn(Optional.of(application));
        when(results.findByRound_IdAndApplication_Id(10L, 22L)).thenReturn(Optional.of(previousResult));
        when(application.getAppliedAt()).thenReturn(java.time.Instant.now());

        var response = service.setFinalOutcome(authentication, 7L, 22L,
                new FinalOutcomeRequest(ApplicationStatus.SELECTED));

        assertEquals(ApplicationStatus.SELECTED, response.status());
        verify(application).setStatus(ApplicationStatus.SELECTED);
        verify(applications).save(application);
    }

    @Test
    void rejectsFinalOutcomeWhenPreviousRoundWasNotPassed() {
        RecruitmentRound finalRound = mock(RecruitmentRound.class);
        RecruitmentRound previousRound = mock(RecruitmentRound.class);
        RoundResult previousResult = mock(RoundResult.class);
        PlacementApplication application = mock(PlacementApplication.class);
        when(finalRound.getOrder()).thenReturn(2);
        when(finalRound.isFinalRound()).thenReturn(true);
        when(finalRound.isResultsPublished()).thenReturn(true);
        when(previousRound.getId()).thenReturn(10L);
        when(previousRound.getOrder()).thenReturn(1);
        when(previousResult.getResult()).thenReturn(RoundResultStatus.FAILED);
        when(rounds.findAllByDrive_IdOrderByOrderAsc(7L)).thenReturn(List.of(previousRound, finalRound));
        when(applications.findById(22L)).thenReturn(Optional.of(application));
        when(application.getDrive()).thenReturn(drive);
        when(results.findByRound_IdAndApplication_Id(10L, 22L)).thenReturn(Optional.of(previousResult));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.setFinalOutcome(authentication, 7L, 22L,
                        new FinalOutcomeRequest(ApplicationStatus.SELECTED)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(applications, never()).save(any());
    }

    @Test
    void rejectsFinalOutcomeWhenFinalRoundIsNotPublished() {
        RecruitmentRound finalRound = mock(RecruitmentRound.class);
        when(finalRound.isFinalRound()).thenReturn(true);
        when(finalRound.isResultsPublished()).thenReturn(false);
        when(rounds.findAllByDrive_IdOrderByOrderAsc(7L)).thenReturn(List.of(finalRound));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.setFinalOutcome(authentication, 7L, 22L,
                        new FinalOutcomeRequest(ApplicationStatus.SELECTED)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void rejectsFinalOutcomeForCancelledDrive() {
        RecruitmentRound finalRound = mock(RecruitmentRound.class);
        when(drive.getStatus()).thenReturn(com.placementmanagement.api.placement.entity.DriveStatus.CANCELLED);
        when(finalRound.isFinalRound()).thenReturn(true);
        when(finalRound.isResultsPublished()).thenReturn(true);
        when(rounds.findAllByDrive_IdOrderByOrderAsc(7L)).thenReturn(List.of(finalRound));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.setFinalOutcome(authentication, 7L, 22L,
                        new FinalOutcomeRequest(ApplicationStatus.SELECTED)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(applications, results);
    }

    @Test
    void rejectsApplicationFromAnotherDrive() {
        RecruitmentRound finalRound = mock(RecruitmentRound.class);
        PlacementApplication application = mock(PlacementApplication.class);
        PlacementDrive anotherDrive = mock(PlacementDrive.class);
        when(finalRound.getId()).thenReturn(11L);
        when(finalRound.isFinalRound()).thenReturn(true);
        when(finalRound.isResultsPublished()).thenReturn(true);
        when(anotherDrive.getId()).thenReturn(99L);
        when(application.getDrive()).thenReturn(anotherDrive);
        when(rounds.findAllByDrive_IdOrderByOrderAsc(7L)).thenReturn(List.of(finalRound));
        when(applications.findById(22L)).thenReturn(Optional.of(application));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.setFinalOutcome(authentication, 7L, 22L,
                        new FinalOutcomeRequest(ApplicationStatus.SELECTED)));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void persistsFinalRejectionForEligibleCandidate() {
        RecruitmentRound finalRound = mock(RecruitmentRound.class);
        PlacementApplication application = mock(PlacementApplication.class);
        RecruitmentRound previousRound = mock(RecruitmentRound.class);
        RoundResult previousResult = mock(RoundResult.class);
        Student student = mock(Student.class);
        when(finalRound.getId()).thenReturn(11L);
        when(finalRound.getOrder()).thenReturn(2);
        when(finalRound.isFinalRound()).thenReturn(true);
        when(finalRound.isResultsPublished()).thenReturn(true);
        when(previousRound.getId()).thenReturn(10L);
        when(previousRound.getOrder()).thenReturn(1);
        when(application.getId()).thenReturn(22L);
        when(application.getDrive()).thenReturn(drive);
        when(application.getStudent()).thenReturn(student);
        when(application.getStatus()).thenReturn(ApplicationStatus.REJECTED);
        when(student.getId()).thenReturn(33L);
        when(previousResult.getResult()).thenReturn(RoundResultStatus.PASSED);
        when(rounds.findAllByDrive_IdOrderByOrderAsc(7L)).thenReturn(List.of(previousRound, finalRound));
        when(applications.findById(22L)).thenReturn(Optional.of(application));
        when(results.findByRound_IdAndApplication_Id(10L, 22L)).thenReturn(Optional.of(previousResult));
        when(application.getAppliedAt()).thenReturn(java.time.Instant.now());

        var response = service.setFinalOutcome(authentication, 7L, 22L,
                new FinalOutcomeRequest(ApplicationStatus.REJECTED));

        assertEquals(ApplicationStatus.REJECTED, response.status());
        verify(application).setStatus(ApplicationStatus.REJECTED);
        verify(applications).save(application);
        verify(results).save(any(RoundResult.class));
    }

    @Test
    void rejectsFinalOutcomeForWithdrawnApplication() {
        RecruitmentRound finalRound = mock(RecruitmentRound.class);
        PlacementApplication application = mock(PlacementApplication.class);
        when(finalRound.getId()).thenReturn(11L);
        when(finalRound.isFinalRound()).thenReturn(true);
        when(finalRound.isResultsPublished()).thenReturn(true);
        when(application.getDrive()).thenReturn(drive);
        when(application.getStatus()).thenReturn(ApplicationStatus.WITHDRAWN);
        when(rounds.findAllByDrive_IdOrderByOrderAsc(7L)).thenReturn(List.of(finalRound));
        when(applications.findById(22L)).thenReturn(Optional.of(application));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.setFinalOutcome(authentication, 7L, 22L,
                        new FinalOutcomeRequest(ApplicationStatus.SELECTED)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("Withdrawn applications cannot receive final outcomes", error.getReason());
    }

    @Test
    void candidateListIncludesApplicationStatusAndSynthesizedFinalResult() {
        RecruitmentRound finalRound = mock(RecruitmentRound.class);
        PlacementApplication application = mock(PlacementApplication.class);
        Student student = mock(Student.class);
        UserAccount studentUser = mock(UserAccount.class);
        when(finalRound.getId()).thenReturn(11L);
        when(finalRound.getOrder()).thenReturn(1);
        when(finalRound.isFinalRound()).thenReturn(true);
        when(rounds.findByIdAndDrive_Id(11L, 7L)).thenReturn(Optional.of(finalRound));
        when(rounds.findAllByDrive_IdOrderByOrderAsc(7L)).thenReturn(List.of(finalRound));
        when(application.getId()).thenReturn(22L);
        when(application.getStatus()).thenReturn(ApplicationStatus.SELECTED);
        when(application.getStudent()).thenReturn(student);
        when(student.getId()).thenReturn(33L);
        when(student.getUser()).thenReturn(studentUser);
        when(studentUser.getFullName()).thenReturn("Candidate Name");
        when(student.getEnrollmentNumber()).thenReturn("ENR-100");
        when(student.getAcademicProgram()).thenReturn("MCA");
        when(applications.findAllByDrive_IdOrderByAppliedAtAsc(7L)).thenReturn(List.of(application));
        when(results.findByRound_IdAndApplication_Id(11L, 22L)).thenReturn(Optional.empty());

        var candidates = service.listCandidates(authentication, 7L, 11L);

        assertEquals(1, candidates.size());
        assertEquals(ApplicationStatus.SELECTED, candidates.get(0).applicationStatus());
        assertEquals(RoundResultStatus.PASSED, candidates.get(0).result());
    }
}
