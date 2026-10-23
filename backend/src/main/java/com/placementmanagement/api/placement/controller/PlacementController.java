package com.placementmanagement.api.placement.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.placementmanagement.api.placement.entity.ApplicationStatus;
import com.placementmanagement.api.placement.dto.PlacementDtos.*;
import com.placementmanagement.api.placement.service.PlacementService;

@RestController
@RequestMapping("/api/placement")
public class PlacementController {
    private final PlacementService service;
    public PlacementController(PlacementService service) { this.service = service; }

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
    @GetMapping("/student/drives")
    public List<DriveResponse> studentDrives(Authentication auth) { return service.listStudentDrives(auth); }
    @PostMapping("/student/drives/{driveId}/applications") @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(@PathVariable Long driveId, Authentication auth) { return service.apply(auth, driveId); }
    @GetMapping("/student/applications")
    public List<ApplicationResponse> applications(Authentication auth) { return service.myApplications(auth); }
    @GetMapping("/applications")
    public StaffApplicationPageResponse staffApplications(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size, @RequestParam(required = false) String search,
            @RequestParam(required = false) ApplicationStatus status, Authentication auth) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination parameters");
        return service.listStaffApplications(auth, page, size, search, status);
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
