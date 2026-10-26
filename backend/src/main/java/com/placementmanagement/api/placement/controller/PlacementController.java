package com.placementmanagement.api.placement.controller;

import java.util.List;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.placementmanagement.api.placement.entity.ApplicationStatus;
import com.placementmanagement.api.placement.dto.PlacementDtos.*;
import com.placementmanagement.api.placement.service.PlacementService;
import com.placementmanagement.api.placement.service.CoordinatorService;
import com.placementmanagement.api.placement.service.RecruitmentService;

@RestController
@RequestMapping("/api/placement")
public class PlacementController {
    private final PlacementService service;
    private final CoordinatorService coordinators;
    private final RecruitmentService recruitment;
    public PlacementController(PlacementService service, CoordinatorService coordinators, RecruitmentService recruitment) {
        this.service = service;
        this.coordinators = coordinators;
        this.recruitment = recruitment;
    }

    @PostMapping("/coordinators") @ResponseStatus(HttpStatus.CREATED)
    public CoordinatorResponse createCoordinator(@RequestBody CoordinatorRequest request, Authentication auth) {
        return coordinators.create(auth, request);
    }
    @GetMapping("/coordinators")
    public List<CoordinatorResponse> listCoordinators(Authentication auth) { return coordinators.list(auth); }
    @GetMapping("/departments")
    public List<com.placementmanagement.api.auth.dto.AuthDtos.DepartmentInfo> departments(Authentication auth) {
        return coordinators.departments(auth);
    }

    @GetMapping("/drives/{driveId}/rounds")
    public List<RoundResponse> rounds(@PathVariable Long driveId, Authentication auth) {
        return recruitment.listRounds(auth, driveId);
    }
    @PostMapping("/drives/{driveId}/rounds") @ResponseStatus(HttpStatus.CREATED)
    public RoundResponse createRound(@PathVariable Long driveId, @RequestBody RoundRequest request, Authentication auth) {
        return recruitment.createRound(auth, driveId, request);
    }
    @PutMapping("/drives/{driveId}/rounds/{roundId}")
    public RoundResponse updateRound(@PathVariable Long driveId, @PathVariable Long roundId,
            @RequestBody RoundRequest request, Authentication auth) {
        return recruitment.updateRound(auth, driveId, roundId, request);
    }
    @DeleteMapping("/drives/{driveId}/rounds/{roundId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRound(@PathVariable Long driveId, @PathVariable Long roundId, Authentication auth) {
        recruitment.deleteRound(auth, driveId, roundId);
    }
    @GetMapping("/drives/{driveId}/rounds/{roundId}/candidates")
    public List<CandidateResponse> candidates(@PathVariable Long driveId, @PathVariable Long roundId, Authentication auth) {
        return recruitment.listCandidates(auth, driveId, roundId);
    }
    @PutMapping("/drives/{driveId}/rounds/{roundId}/results/{applicationId}")
    public RoundResultResponse result(@PathVariable Long driveId, @PathVariable Long roundId,
            @PathVariable Long applicationId, @RequestBody RoundResultRequest request, Authentication auth) {
        return recruitment.recordResult(auth, driveId, roundId, applicationId, request);
    }
    @PostMapping("/drives/{driveId}/rounds/{roundId}/publish")
    public RoundResponse publishRound(@PathVariable Long driveId, @PathVariable Long roundId, Authentication auth) {
        return recruitment.publishRound(auth, driveId, roundId);
    }
    @PutMapping("/drives/{driveId}/applications/{applicationId}/outcome")
    public ApplicationResponse finalOutcome(@PathVariable Long driveId, @PathVariable Long applicationId,
            @RequestBody FinalOutcomeRequest request, Authentication auth) {
        return recruitment.setFinalOutcome(auth, driveId, applicationId, request);
    }
    @GetMapping("/student/applications/progress")
    public List<StudentApplicationProgress> studentProgress(Authentication auth) {
        return recruitment.studentProgress(auth);
    }

    @PostMapping("/companies") @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse createCompany(@RequestBody CompanyRequest request, Authentication auth) { return service.createCompany(auth, request); }
    @GetMapping("/companies")
    public List<CompanyResponse> companies(Authentication auth) { return service.listCompanies(auth); }
    @PostMapping("/drives") @ResponseStatus(HttpStatus.CREATED)
    public DriveResponse createDrive(@RequestBody DriveRequest request, Authentication auth) { return service.createDrive(auth, request); }
    @GetMapping("/drives")
    public List<DriveResponse> staffDrives(Authentication auth) { return service.listStaffDrives(auth); }
    @PostMapping("/drives/{driveId}/publish")
    public DriveResponse publishDrive(@PathVariable Long driveId, Authentication auth) { return service.publishDrive(auth, driveId); }
    @PostMapping("/drives/{driveId}/cancel")
    public DriveResponse cancelDrive(@PathVariable Long driveId, Authentication auth) { return service.cancelDrive(auth, driveId); }
    @GetMapping("/student/drives")
    public List<DriveResponse> studentDrives(Authentication auth) { return service.listStudentDrives(auth); }
    @PostMapping("/student/drives/{driveId}/applications") @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(@PathVariable Long driveId, Authentication auth) { return service.apply(auth, driveId); }
    @GetMapping("/student/applications")
    public List<ApplicationResponse> applications(Authentication auth) { return service.myApplications(auth); }
    @GetMapping("/applications")
    public StaffApplicationPageResponse staffApplications(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size, @RequestParam(required = false) String search,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) BigDecimal minCgpa,
            @RequestParam(required = false) BigDecimal maxCgpa, Authentication auth) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
        return service.listStaffApplications(auth, page, size, search, status, companyId, minCgpa, maxCgpa);
    }
    @GetMapping("/students")
    public StaffStudentPageResponse staffStudents(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size, @RequestParam(required = false) String search, Authentication auth) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
        return service.listStaffStudents(auth, page, size, search);
    }
    @GetMapping("/dashboard/summary")
    public DashboardSummaryResponse dashboardSummary(Authentication auth) { return service.dashboardSummary(auth); }
}
