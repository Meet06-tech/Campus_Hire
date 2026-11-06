package com.placementmanagement.api.placement.controller;

import java.util.List;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.placementmanagement.api.placement.entity.ApplicationStatus;
import com.placementmanagement.api.placement.dto.PlacementDtos.*;
import com.placementmanagement.api.placement.service.PlacementService;
import com.placementmanagement.api.placement.service.CoordinatorService;
import com.placementmanagement.api.placement.service.RecruitmentService;
import com.placementmanagement.api.placement.service.PlacementReportService;

@RestController
@RequestMapping("/api/placement")
public class PlacementController {
    private final PlacementService service;
    private final CoordinatorService coordinators;
    private final RecruitmentService recruitment;
    private final PlacementReportService reports;
    public PlacementController(PlacementService service, CoordinatorService coordinators, RecruitmentService recruitment, PlacementReportService reports) {
        this.service = service;
        this.coordinators = coordinators;
        this.recruitment = recruitment;
        this.reports = reports;
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
    @GetMapping("/student/applications/history")
    public List<StudentApplicationProgress> studentHistory(Authentication auth) {
        return recruitment.studentHistory(auth);
    }

    @PostMapping("/companies") @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse createCompany(@RequestBody CompanyRequest request, Authentication auth) { return service.createCompany(auth, request); }
    @GetMapping("/companies")
    public List<CompanyResponse> companies(Authentication auth) { return service.listCompanies(auth); }
    @PutMapping("/companies/{companyId}")
    public CompanyResponse updateCompany(@PathVariable Long companyId, @RequestBody CompanyRequest request, Authentication auth) { return service.updateCompany(auth, companyId, request); }
    @PostMapping("/drives") @ResponseStatus(HttpStatus.CREATED)
    public DriveResponse createDrive(@RequestBody DriveRequest request, Authentication auth) { return service.createDrive(auth, request); }
    @GetMapping("/drives")
    public List<DriveResponse> staffDrives(Authentication auth) { return service.listStaffDrives(auth); }
    @GetMapping("/round-management/drives")
    public List<DriveResponse> roundManagementDrives(Authentication auth) { return service.listRoundManagementDrives(auth); }
    @PostMapping("/drives/{driveId}/publish")
    public DriveResponse publishDrive(@PathVariable Long driveId, Authentication auth) { return service.publishDrive(auth, driveId); }
    @PostMapping("/drives/{driveId}/cancel")
    public DriveResponse cancelDrive(@PathVariable Long driveId, Authentication auth) { return service.cancelDrive(auth, driveId); }
    @PostMapping("/drives/{driveId}/complete")
    public DriveResponse completeDrive(@PathVariable Long driveId, Authentication auth) { return service.completeDrive(auth, driveId); }
    @PostMapping("/drives/{driveId}/archive")
    public DriveResponse archiveDrive(@PathVariable Long driveId, Authentication auth) { return service.archiveDrive(auth, driveId); }

    @GetMapping(value = "/reports/placed.csv", produces = "text/csv")
    public ResponseEntity<byte[]> placedCsv(@RequestParam(required = false) Long departmentId, Authentication auth) { return report(reports.placed(auth, departmentId, false), "placed-students.csv", "text/csv"); }
    @GetMapping(value = "/reports/placed.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> placedXlsx(@RequestParam(required = false) Long departmentId, Authentication auth) { return report(reports.placed(auth, departmentId, true), "placed-students.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); }
    @GetMapping(value = "/reports/visited-companies.csv", produces = "text/csv")
    public ResponseEntity<byte[]> visitedCompaniesCsv(@RequestParam(required = false) Long departmentId, Authentication auth) { return report(reports.visitedCompanies(auth, departmentId, false), "visited-companies.csv", "text/csv"); }
    @GetMapping(value = "/reports/visited-companies.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> visitedCompaniesXlsx(@RequestParam(required = false) Long departmentId, Authentication auth) { return report(reports.visitedCompanies(auth, departmentId, true), "visited-companies.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); }
    @GetMapping(value = "/reports/company-hiring.csv", produces = "text/csv")
    public ResponseEntity<byte[]> companyHiringCsv(@RequestParam(required = false) Long departmentId, Authentication auth) { return report(reports.companyHiring(auth, departmentId, false), "company-hiring.csv", "text/csv"); }
    @GetMapping(value = "/reports/company-hiring.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> companyHiringXlsx(@RequestParam(required = false) Long departmentId, Authentication auth) { return report(reports.companyHiring(auth, departmentId, true), "company-hiring.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); }
    @GetMapping(value = "/reports/students.csv", produces = "text/csv")
    public ResponseEntity<byte[]> studentsCsv(@RequestParam(required = false) Long departmentId, Authentication auth) { return report(reports.students(auth, departmentId, false), "registered-students.csv", "text/csv"); }
    @GetMapping(value = "/reports/students.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> studentsXlsx(@RequestParam(required = false) Long departmentId, Authentication auth) { return report(reports.students(auth, departmentId, true), "registered-students.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); }

    private ResponseEntity<byte[]> report(byte[] body, String filename, String type) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(type))
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"").body(body);
    }
    @GetMapping("/student/drives")
    public List<DriveResponse> studentDrives(Authentication auth) { return service.listStudentDrives(auth); }
    @PostMapping("/student/drives/{driveId}/applications") @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(@PathVariable Long driveId, Authentication auth) { return service.apply(auth, driveId); }
    @PostMapping("/drives/{driveId}/applications/{applicationId}/reject")
    public ApplicationResponse rejectApplication(@PathVariable Long driveId, @PathVariable Long applicationId,
            Authentication auth) { return service.rejectApplication(auth, driveId, applicationId); }
    @GetMapping("/student/applications")
    public List<ApplicationResponse> applications(Authentication auth) { return service.myApplications(auth); }
    @PutMapping("/student/applications/{applicationId}/offer")
    public ApplicationResponse decideOffer(@PathVariable Long applicationId,
            @RequestBody OfferDecisionRequest request, Authentication auth) {
        return service.decideOffer(auth, applicationId, request);
    }
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
