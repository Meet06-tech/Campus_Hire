package com.placementmanagement.api.placement.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.Student;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.placement.dto.PlacementDtos.*;
import com.placementmanagement.api.placement.entity.*;
import com.placementmanagement.api.placement.repository.PlacementApplicationRepository;
import com.placementmanagement.api.placement.repository.PlacementDriveRepository;
import com.placementmanagement.api.placement.repository.RecruitmentRoundRepository;
import com.placementmanagement.api.placement.repository.RoundResultRepository;

@Service
public class RecruitmentService {
    private final PlacementDriveRepository drives;
    private final PlacementApplicationRepository applications;
    private final RecruitmentRoundRepository rounds;
    private final RoundResultRepository results;
    private final PlacementAuthorizationService authorization;

    public RecruitmentService(PlacementDriveRepository drives, PlacementApplicationRepository applications,
            RecruitmentRoundRepository rounds, RoundResultRepository results,
            PlacementAuthorizationService authorization) {
        this.drives = drives;
        this.applications = applications;
        this.rounds = rounds;
        this.results = results;
        this.authorization = authorization;
    }

    @Transactional(readOnly = true)
    public List<RoundResponse> listRounds(Authentication auth, Long driveId) {
        PlacementDrive drive = managedDrive(auth, driveId);
        return rounds.findAllByDrive_IdOrderByOrderAsc(driveId).stream()
                .map(round -> roundResponse(round, candidateApplications(round, drive))).toList();
    }

    @Transactional
    public RoundResponse createRound(Authentication auth, Long driveId, RoundRequest request) {
        PlacementDrive drive = managedDrive(auth, driveId);
        validateRoundRequest(request);
        ensureStructureEditable(driveId);
        boolean finalRound = Boolean.TRUE.equals(request.finalRound());
        if (finalRound && rounds.existsByDrive_IdAndFinalRoundTrue(driveId)) throw conflict("A final round already exists");
        RecruitmentRound round = rounds.save(new RecruitmentRound(drive, request.order(), request.name().trim(), finalRound));
        return roundResponse(round, candidateApplications(round, drive));
    }

    @Transactional
    public RoundResponse updateRound(Authentication auth, Long driveId, Long roundId, RoundRequest request) {
        PlacementDrive drive = managedDrive(auth, driveId);
        validateRoundRequest(request);
        ensureStructureEditable(driveId);
        RecruitmentRound round = findRound(driveId, roundId);
        if (Boolean.TRUE.equals(request.finalRound()) && !round.isFinalRound()
                && rounds.existsByDrive_IdAndFinalRoundTrue(driveId)) throw conflict("A final round already exists");
        round.update(request.order(), request.name().trim(), Boolean.TRUE.equals(request.finalRound()));
        return roundResponse(round, candidateApplications(round, drive));
    }

    @Transactional
    public void deleteRound(Authentication auth, Long driveId, Long roundId) {
        managedDrive(auth, driveId);
        ensureStructureEditable(driveId);
        rounds.delete(findRound(driveId, roundId));
    }

    @Transactional(readOnly = true)
    public List<CandidateResponse> listCandidates(Authentication auth, Long driveId, Long roundId) {
        PlacementDrive drive = managedDrive(auth, driveId);
        RecruitmentRound round = findRound(driveId, roundId);
        return candidateApplications(round, drive).stream().map(application -> candidate(round, application)).toList();
    }

    @Transactional
    public RoundResultResponse recordResult(Authentication auth, Long driveId, Long roundId,
            Long applicationId, RoundResultRequest request) {
        PlacementDrive drive = managedDrive(auth, driveId);
        RecruitmentRound round = findRound(driveId, roundId);
        if (request == null || request.result() == null) throw bad("Round result is required");
        PlacementApplication application = applications.findById(applicationId)
                .orElseThrow(() -> notFound("Application"));
        if (!Objects.equals(application.getDrive().getId(), driveId)) throw notFound("Application");
        if (!candidateApplications(round, drive).contains(application)) throw bad("Student is not eligible for this round");
        RoundResult result = results.findByRound_IdAndApplication_Id(roundId, applicationId)
                .orElseGet(() -> new RoundResult(round, application));
        if (result.getResult() != request.result() && hasLaterRecordedResult(applicationId, round.getOrder()))
            throw conflict("Later round results must be cleared before correcting this result");
        UserAccount recorder = authorization.staff(auth);
        result.update(request.result(), recorder);
        result = results.save(result);
        return new RoundResultResponse(roundId, applicationId, result.getResult(), result.getRecordedAt());
    }

    @Transactional
    public RoundResponse publishRound(Authentication auth, Long driveId, Long roundId) {
        PlacementDrive drive = managedDrive(auth, driveId);
        RecruitmentRound round = findRound(driveId, roundId);
        round.publishResults();
        return roundResponse(round, candidateApplications(round, drive));
    }

    @Transactional
    public void setFinalOutcome(Authentication auth, Long driveId, Long applicationId, FinalOutcomeRequest request) {
        PlacementDrive drive = managedDrive(auth, driveId);
        if (request == null || (request.outcome() != ApplicationStatus.SELECTED && request.outcome() != ApplicationStatus.REJECTED))
            throw bad("Final outcome must be SELECTED or REJECTED");
        RecruitmentRound finalRound = rounds.findAllByDrive_IdOrderByOrderAsc(driveId).stream()
                .filter(RecruitmentRound::isFinalRound).findFirst()
                .orElseThrow(() -> bad("A final round must be configured"));
        if (!finalRound.isResultsPublished()) throw bad("Publish the final round before recording outcomes");
        PlacementApplication application = applications.findById(applicationId)
                .orElseThrow(() -> notFound("Application"));
        if (!Objects.equals(application.getDrive().getId(), drive.getId())) throw notFound("Application");
        RoundResult finalResult = results.findByRound_IdAndApplication_Id(finalRound.getId(), applicationId)
                .orElseThrow(() -> bad("Record a final-round result first"));
        if (finalResult.getResult() != RoundResultStatus.PASSED) throw bad("Only a student who passed the final round can receive an outcome");
        application.setStatus(request.outcome());
    }

    @Transactional(readOnly = true)
    public List<StudentApplicationProgress> studentProgress(Authentication auth) {
        Student student = authorization.student(auth);
        return applications.findAllByStudent_IdOrderByAppliedAtDesc(student.getId()).stream()
                .map(this::progress).toList();
    }

    private StudentApplicationProgress progress(PlacementApplication application) {
        List<StudentRoundProgress> progress = rounds.findAllByDrive_IdOrderByOrderAsc(application.getDrive().getId()).stream()
                .map(round -> {
                    Optional<RoundResult> result = results.findByRound_IdAndApplication_Id(round.getId(), application.getId());
                    String status = !round.isResultsPublished() ? "UPCOMING"
                            : result.map(value -> switch (value.getResult()) {
                                case PASSED -> "CLEARED";
                                case FAILED -> "NOT_CLEARED";
                                case PENDING -> "AWAITING_RESULT";
                            }).orElse("AWAITING_RESULT");
                    return new StudentRoundProgress(round.getId(), round.getOrder(), round.getName(), round.isResultsPublished(), status);
                }).toList();
        String finalOutcome = application.getStatus() == ApplicationStatus.SELECTED ? "SELECTED"
                : application.getStatus() == ApplicationStatus.REJECTED ? "NOT_SELECTED" : null;
        return new StudentApplicationProgress(application.getId(), application.getDrive().getId(),
                application.getDrive().getCompany().getName(), application.getDrive().getTitle(), application.getAppliedAt(),
                application.getStatus(), finalOutcome, progress);
    }

    private List<PlacementApplication> candidateApplications(RecruitmentRound round, PlacementDrive drive) {
        return applications.findAllByDrive_IdOrderByAppliedAtAsc(drive.getId()).stream()
                .filter(application -> application.getStatus() != ApplicationStatus.WITHDRAWN)
                .filter(application -> round.getOrder() == 1 || passedPreviousRound(application, drive, round))
                .toList();
    }

    private boolean passedPreviousRound(PlacementApplication application, PlacementDrive drive, RecruitmentRound current) {
        return rounds.findAllByDrive_IdOrderByOrderAsc(drive.getId()).stream()
                .filter(round -> round.getOrder() == current.getOrder() - 1)
                .findFirst()
                .flatMap(round -> results.findByRound_IdAndApplication_Id(round.getId(), application.getId()))
                .map(result -> result.getResult() == RoundResultStatus.PASSED).orElse(false);
    }

    private CandidateResponse candidate(RecruitmentRound round, PlacementApplication application) {
        Student student = application.getStudent();
        Optional<RoundResult> result = results.findByRound_IdAndApplication_Id(round.getId(), application.getId());
        return new CandidateResponse(application.getId(), student.getId(), student.getUser().getFullName(),
                student.getEnrollmentNumber(), student.getAcademicProgram(), result.map(RoundResult::getResult).orElse(RoundResultStatus.PENDING));
    }

    private RoundResponse roundResponse(RecruitmentRound round, List<PlacementApplication> candidates) {
        Map<RoundResultStatus, Long> counts = candidates.stream()
                .map(application -> results.findByRound_IdAndApplication_Id(round.getId(), application.getId())
                        .map(RoundResult::getResult).orElse(RoundResultStatus.PENDING))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return new RoundResponse(round.getId(), round.getDrive().getId(), round.getOrder(), round.getName(), round.isFinalRound(),
                round.isResultsPublished(), candidates.size(), counts.getOrDefault(RoundResultStatus.PASSED, 0L),
                counts.getOrDefault(RoundResultStatus.FAILED, 0L), counts.getOrDefault(RoundResultStatus.PENDING, 0L));
    }

    private PlacementDrive managedDrive(Authentication auth, Long driveId) {
        UserAccount user = authorization.staff(auth);
        PlacementDrive drive = drives.findById(driveId).orElseThrow(() -> notFound("Placement drive"));
        if (user.getRole() == UserRole.DTPO) {
            Department department = user.getDepartment();
            if (department == null || drive.getDepartment() == null || !department.getId().equals(drive.getDepartment().getId()))
                throw forbidden("Drive is outside your department");
        }
        return drive;
    }

    private RecruitmentRound findRound(Long driveId, Long roundId) {
        return rounds.findByIdAndDrive_Id(roundId, driveId).orElseThrow(() -> notFound("Round"));
    }

    private void ensureStructureEditable(Long driveId) {
        boolean hasResults = rounds.findAllByDrive_IdOrderByOrderAsc(driveId).stream()
                .anyMatch(round -> !results.findAllByRound_Id(round.getId()).isEmpty());
        if (hasResults) throw conflict("Round structure cannot change after results are recorded");
    }

    private boolean hasLaterRecordedResult(Long applicationId, Integer order) {
        return results.findAllByApplication_IdOrderByRound_OrderAsc(applicationId).stream()
                .anyMatch(result -> result.getRound().getOrder() > order && result.getResult() != RoundResultStatus.PENDING);
    }

    private void validateRoundRequest(RoundRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) throw bad("Round name is required");
        if (request.order() == null || request.order() < 1) throw bad("Round order must be positive");
    }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private ResponseStatusException forbidden(String message) { return new ResponseStatusException(HttpStatus.FORBIDDEN, message); }
    private ResponseStatusException notFound(String type) { return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " not found"); }
}
