package com.placementmanagement.api.placement.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.entity.Student;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.auth.repository.StudentRepository;
import com.placementmanagement.api.placement.entity.ApplicationStatus;
import com.placementmanagement.api.placement.entity.PlacementApplication;
import com.placementmanagement.api.placement.entity.PlacementDrive;
import com.placementmanagement.api.placement.entity.RecruitmentRound;
import com.placementmanagement.api.placement.entity.RoundResult;
import com.placementmanagement.api.placement.entity.RoundResultStatus;
import com.placementmanagement.api.placement.repository.PlacementApplicationRepository;
import com.placementmanagement.api.placement.repository.PlacementDriveRepository;
import com.placementmanagement.api.placement.repository.AcademicYearRepository;
import com.placementmanagement.api.placement.repository.RecruitmentRoundRepository;
import com.placementmanagement.api.placement.repository.RoundResultRepository;
import com.placementmanagement.api.placement.entity.AcademicYear;
import com.placementmanagement.api.placement.repository.PlacementApplicationRepository.CompanyHiringProjection;

@Service
public class PlacementReportService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);
    private final PlacementAuthorizationService authorization;
    private final PlacementApplicationRepository applications;
    private final PlacementDriveRepository drives;
    private final StudentRepository students;
    private final AcademicYearRepository academicYears;
    private final RecruitmentRoundRepository rounds;
    private final RoundResultRepository roundResults;

    public PlacementReportService(PlacementAuthorizationService authorization,
            PlacementApplicationRepository applications, PlacementDriveRepository drives, StudentRepository students,
            AcademicYearRepository academicYears, RecruitmentRoundRepository rounds, RoundResultRepository roundResults) {
        this.authorization = authorization;
        this.applications = applications;
        this.drives = drives;
        this.students = students;
        this.academicYears = academicYears;
        this.rounds = rounds;
        this.roundResults = roundResults;
    }

    public List<AcademicYear> years(Authentication auth) {
        authorization.staff(auth);
        return academicYears.findAllByOrderByLabelDesc();
    }

    public byte[] placed(Authentication auth, Long departmentId, boolean excel) {
        return placed(auth, departmentId, null, excel);
    }

    public byte[] placed(Authentication auth, Long departmentId, Long academicYearId, boolean excel) {
        UserAccount user = authorization.staff(auth);
        departmentId = scopedDepartment(user, departmentId);
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Student name", "Enrollment number", "Email", "Department", "Program", "Semester",
                "Company", "Drive", "Status", "CTC", "Applied at", "Placement date"));
        placedApplications(departmentId).stream()
                .filter(a -> inScope(user, a.getDrive()) && inYear(a.getDrive(), academicYearId)).forEach(a -> rows.add(placedRow(a)));
        return render(rows, excel);
    }

    public byte[] visitedCompanies(Authentication auth, Long departmentId, boolean excel) {
        return visitedCompanies(auth, departmentId, null, excel);
    }

    public byte[] visitedCompanies(Authentication auth, Long departmentId, Long academicYearId, boolean excel) {
        UserAccount user = authorization.staff(auth);
        departmentId = scopedDepartment(user, departmentId);
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Company", "Website", "Contact email", "Drive", "Drive status", "Deadline", "Employment type", "CTC", "Stipend"));
        visitedDrives(user, departmentId).stream().filter(d -> inYear(d, academicYearId)).forEach(d -> rows.add(List.of(
                d.getCompany().getName(), value(d.getCompany().getWebsite()),
                value(d.getCompany().getHrContactEmail()), value(d.getTitle()), d.getStatus().name(),
                formatDate(d.getApplicationDeadline()), value(d.getEmploymentType()),
                formatCtc(d.getCtcAmount(), d.getCtcCurrency()), value(d.getStipend()))));
        return render(rows, excel);
    }

    public byte[] companyHiring(Authentication auth, Long departmentId, boolean excel) {
        return companyHiring(auth, departmentId, null, excel);
    }

    public byte[] companyHiring(Authentication auth, Long departmentId, Long academicYearId, boolean excel) {
        UserAccount user = authorization.staff(auth);
        departmentId = scopedDepartment(user, departmentId);
        List<CompanyHiringProjection> grouped = user.getRole() == UserRole.DTPO
                ? hiringByDriveDepartment(departmentId, academicYearId)
                : departmentId == null ? hiringAll(academicYearId) : hiringByStudentDepartment(departmentId, academicYearId);
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Company", "Department", "Students hired", "Placement drive", "CTC", "Placement date"));
        Map<Long, Long> totals = new LinkedHashMap<>();
        Map<Long, String> companyNames = new LinkedHashMap<>();
        grouped.forEach(row -> { totals.merge(row.getCompanyId(), row.getHiredCount(), Long::sum); companyNames.put(row.getCompanyId(), row.getCompanyName()); rows.add(List.of(value(row.getCompanyName()), value(row.getDepartmentName()),
                value(row.getHiredCount()), value(row.getDriveTitle()), formatCtc(row.getCtcAmount(), row.getCtcCurrency()),
                formatDate(row.getPlacementDate()))); });
        totals.forEach((companyId, count) -> rows.add(List.of(companyNames.get(companyId), "Overall", value(count), "All drives", "", "")));
        return render(rows, excel);
    }

    public byte[] students(Authentication auth, Long departmentId, boolean excel) {
        return students(auth, departmentId, null, excel);
    }

    public byte[] students(Authentication auth, Long departmentId, Long academicYearId, boolean excel) {
        UserAccount user = authorization.staff(auth);
        if (user.getRole() == UserRole.DTPO && !Objects.equals(user.getDepartment().getId(), departmentId == null ? user.getDepartment().getId() : departmentId))
            throw forbidden("DTPO cannot export another department");
        List<Student> source = departmentId == null ? students.findAll() : students.findAllByDepartment_IdOrderByUser_FullNameAsc(departmentId);
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Student name", "Enrollment number", "Email", "Phone", "Department", "Program", "Semester", "Graduation year", "CGPA", "Academic year"));
        source.stream().filter(s -> departmentId == null || s.getDepartment().getId().equals(departmentId))
                .filter(s -> user.getRole() == UserRole.UTPO || s.getDepartment().getId().equals(user.getDepartment().getId()))
                .forEach(s -> rows.add(List.of(s.getUser().getFullName(), s.getEnrollmentNumber(), s.getUser().getUniversityEmail(),
                        value(s.getPhoneNumber()), s.getDepartment().getName(), value(s.getAcademicProgram()), value(s.getCurrentSemester()),
                        value(s.getGraduationYear()), value(s.getCgpa()), academicYearId == null ? "All years" : "Not applicable: no student registration year exists")));
        return render(rows, excel);
    }

    public byte[] placementStatistics(Authentication auth, Long departmentId, Long academicYearId, boolean excel) {
        UserAccount user = authorization.staff(auth);
        departmentId = scopedDepartment(user, departmentId);
        List<PlacementApplication> scopedApplications = scopedApplications(user, departmentId, academicYearId);
        List<Student> scopedStudents = scopedStudents(user, departmentId);
        long placed = scopedApplications.stream().filter(a -> a.getStatus() == ApplicationStatus.SELECTED).map(a -> a.getStudent().getId()).distinct().count();
        List<PlacementDrive> scopedDrives = scopedDrives(user, departmentId, academicYearId);
        List<BigDecimal> packages = scopedDrives.stream().map(PlacementDrive::getCtcAmount).filter(Objects::nonNull).toList();
        long companies = scopedDrives.stream().map(d -> d.getCompany().getId()).distinct().count();
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Academic year", "Eligible students", "Placed students", "Unplaced students", "Placement percentage", "Companies", "Drives", "Average package", "Highest package", "Lowest package"));
        rows.add(List.of(yearLabel(academicYearId), value(scopedStudents.size()), value(placed), value(Math.max(0, scopedStudents.size() - placed)),
                scopedStudents.isEmpty() ? "0%" : String.format("%.2f%%", placed * 100.0 / scopedStudents.size()), value(companies), value(scopedDrives.size()),
                formatPackageAverage(packages), packages.isEmpty() ? "" : formatCtc(packages.stream().max(BigDecimal::compareTo).orElse(null), "INR"),
                packages.isEmpty() ? "" : formatCtc(packages.stream().min(BigDecimal::compareTo).orElse(null), "INR")));
        return render(rows, excel);
    }

    public byte[] departmentPerformance(Authentication auth, Long departmentId, Long academicYearId, boolean excel) {
        UserAccount user = authorization.staff(auth);
        departmentId = scopedDepartment(user, departmentId);
        List<Student> source = scopedStudents(user, departmentId);
        Map<Long, List<Student>> byDepartment = new LinkedHashMap<>();
        source.forEach(s -> byDepartment.computeIfAbsent(s.getDepartment().getId(), ignored -> new ArrayList<>()).add(s));
        List<PlacementApplication> selected = scopedApplications(user, departmentId, academicYearId).stream().filter(a -> a.getStatus() == ApplicationStatus.SELECTED).toList();
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Department", "Eligible students", "Placed students", "Unplaced students", "Placement percentage", "Companies", "Average package", "Highest package"));
        byDepartment.forEach((id, departmentStudents) -> {
            List<PlacementApplication> deptSelected = selected.stream().filter(a -> a.getStudent().getDepartment().getId().equals(id)).toList();
            List<BigDecimal> packages = deptSelected.stream().map(a -> a.getDrive().getCtcAmount()).filter(Objects::nonNull).toList();
            long count = deptSelected.stream().map(a -> a.getStudent().getId()).distinct().count();
            rows.add(List.of(departmentStudents.get(0).getDepartment().getName(), value(departmentStudents.size()), value(count), value(Math.max(0, departmentStudents.size() - count)),
                    departmentStudents.isEmpty() ? "0%" : String.format("%.2f%%", count * 100.0 / departmentStudents.size()),
                    value(deptSelected.stream().map(a -> a.getDrive().getCompany().getId()).distinct().count()), formatPackageAverage(packages),
                    packages.isEmpty() ? "" : formatCtc(packages.stream().max(BigDecimal::compareTo).orElse(null), "INR")));
        });
        return render(rows, excel);
    }

    public byte[] packageAnalysis(Authentication auth, Long departmentId, Long academicYearId, boolean excel) {
        UserAccount user = authorization.staff(auth);
        departmentId = scopedDepartment(user, departmentId);
        List<PlacementApplication> selected = scopedApplications(user, departmentId, academicYearId).stream().filter(a -> a.getStatus() == ApplicationStatus.SELECTED).toList();
        Map<Long, List<PlacementApplication>> groups = new LinkedHashMap<>();
        selected.forEach(a -> groups.computeIfAbsent(a.getDrive().getId(), ignored -> new ArrayList<>()).add(a));
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Company", "Placement drive", "Department", "Annual package", "Selected students"));
        groups.values().forEach(group -> { PlacementDrive d = group.get(0).getDrive(); rows.add(List.of(d.getCompany().getName(), d.getTitle(), d.getDepartment() == null ? "University-wide" : d.getDepartment().getName(), formatCtc(d.getCtcAmount(), d.getCtcCurrency()), value(group.stream().map(a -> a.getStudent().getId()).distinct().count()))); });
        return render(rows, excel);
    }

    public byte[] driveParticipation(Authentication auth, Long departmentId, Long academicYearId, boolean excel) {
        UserAccount user = authorization.staff(auth);
        departmentId = scopedDepartment(user, departmentId);
        List<PlacementApplication> scoped = scopedApplications(user, departmentId, academicYearId);
        Map<Long, List<PlacementApplication>> groups = new LinkedHashMap<>();
        scoped.forEach(a -> groups.computeIfAbsent(a.getDrive().getId(), ignored -> new ArrayList<>()).add(a));
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Company", "Placement drive", "Academic year", "Department", "Applicants", "Selected students", "Rejected students", "Drive status"));
        groups.values().forEach(group -> { PlacementDrive d = group.get(0).getDrive(); rows.add(List.of(d.getCompany().getName(), d.getTitle(), d.getAcademicYear().getLabel(), d.getDepartment() == null ? "University-wide" : d.getDepartment().getName(), value(group.stream().map(a -> a.getStudent().getId()).distinct().count()), value(group.stream().filter(a -> a.getStatus() == ApplicationStatus.SELECTED).map(a -> a.getStudent().getId()).distinct().count()), value(group.stream().filter(a -> a.getStatus() == ApplicationStatus.REJECTED || a.getStatus() == ApplicationStatus.APPLICATION_REJECTED).map(a -> a.getStudent().getId()).distinct().count()), d.getStatus().name())); });
        return render(rows, excel);
    }

    public byte[] roundResults(Authentication auth, Long departmentId, Long academicYearId, boolean excel) {
        UserAccount user = authorization.staff(auth);
        departmentId = scopedDepartment(user, departmentId);
        final Long scopedDepartmentId = departmentId;
        List<RoundResult> results = roundResults.findAll().stream().filter(r -> inScope(user, r.getRound().getDrive()) && inYear(r.getRound().getDrive(), academicYearId)
                && (scopedDepartmentId == null || r.getApplication().getStudent().getDepartment().getId().equals(scopedDepartmentId))).toList();
        Map<Long, List<RoundResult>> groups = new LinkedHashMap<>();
        results.forEach(r -> groups.computeIfAbsent(r.getRound().getId(), ignored -> new ArrayList<>()).add(r));
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Company", "Drive", "Round", "Department", "Students participating", "Passed", "Failed", "Pending"));
        groups.values().forEach(group -> { RecruitmentRound round = group.get(0).getRound(); rows.add(List.of(round.getDrive().getCompany().getName(), round.getDrive().getTitle(), round.getName(), round.getDrive().getDepartment() == null ? "University-wide" : round.getDrive().getDepartment().getName(), value(group.size()), value(group.stream().filter(r -> r.getResult() == RoundResultStatus.PASSED).count()), value(group.stream().filter(r -> r.getResult() == RoundResultStatus.FAILED).count()), value(group.stream().filter(r -> r.getResult() == RoundResultStatus.PENDING).count()))); });
        return render(rows, excel);
    }

    public byte[] unplacedStudents(Authentication auth, Long departmentId, Long academicYearId, boolean excel) {
        UserAccount user = authorization.staff(auth);
        departmentId = scopedDepartment(user, departmentId);
        List<Student> source = scopedStudents(user, departmentId);
        List<PlacementApplication> scoped = scopedApplications(user, departmentId, academicYearId);
        java.util.Set<Long> placed = scoped.stream().filter(a -> a.getStatus() == ApplicationStatus.SELECTED).map(a -> a.getStudent().getId()).collect(java.util.stream.Collectors.toSet());
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Enrollment number", "Student name", "Department", "Applications submitted", "Latest application status"));
        source.stream().filter(s -> !placed.contains(s.getId())).forEach(s -> { List<PlacementApplication> own = scoped.stream().filter(a -> a.getStudent().getId().equals(s.getId())).toList(); String latest = own.isEmpty() ? "NO_APPLICATION" : own.get(own.size() - 1).getStatus().name(); rows.add(List.of(s.getEnrollmentNumber(), s.getUser().getFullName(), s.getDepartment().getName(), value(own.size()), latest)); });
        return render(rows, excel);
    }

    private List<PlacementApplication> placedApplications(Long departmentId) {
        return departmentId == null
                ? applications.findAllByStatusOrderByAppliedAtAsc(ApplicationStatus.SELECTED)
                : applications.findAllByStatusAndStudent_Department_IdOrderByAppliedAtAsc(ApplicationStatus.SELECTED, departmentId);
    }

    private List<PlacementDrive> visitedDrives(UserAccount user, Long departmentId) {
        if (departmentId == null) {
            return user.getRole() == UserRole.DTPO
                    ? drives.findAllByDepartment_IdOrderByApplicationDeadlineAsc(user.getDepartment().getId())
                    : drives.findAllByOrderByApplicationDeadlineAsc();
        }
        List<PlacementDrive> scoped = drives.findAllByDepartment_IdOrderByApplicationDeadlineAsc(departmentId);
        if (user.getRole() == UserRole.UTPO) {
            var universityWideDriveIds = applications.findDistinctDriveIdsByStudentDepartmentId(departmentId);
            drives.findAllByOrderByApplicationDeadlineAsc().stream()
                    .filter(d -> d.getDepartment() == null && universityWideDriveIds.contains(d.getId()))
                    .forEach(scoped::add);
        }
        return scoped;
    }

    private Long scopedDepartment(UserAccount user, Long requestedDepartmentId) {
        if (user.getRole() == UserRole.DTPO) {
            Long ownDepartmentId = user.getDepartment() == null ? null : user.getDepartment().getId();
            if (ownDepartmentId == null || (requestedDepartmentId != null && !ownDepartmentId.equals(requestedDepartmentId)))
                throw forbidden("DTPO cannot export another department");
            return ownDepartmentId;
        }
        return requestedDepartmentId;
    }

    private List<String> placedRow(PlacementApplication a) {
        Student s = a.getStudent();
        PlacementDrive d = a.getDrive();
        return List.of(s.getUser().getFullName(), s.getEnrollmentNumber(), s.getUser().getUniversityEmail(), s.getDepartment().getName(),
                value(s.getAcademicProgram()), value(s.getCurrentSemester()), d.getCompany().getName(), d.getTitle(), a.getStatus().name(),
                formatCtc(d.getCtcAmount(), d.getCtcCurrency()), formatDate(a.getAppliedAt()), formatDate(a.getPlacementSelectedAt()));
    }

    private boolean inScope(UserAccount user, PlacementDrive drive) {
        return user.getRole() == UserRole.UTPO || (drive.getDepartment() != null && user.getDepartment() != null
                && drive.getDepartment().getId().equals(user.getDepartment().getId()));
    }

    private boolean inYear(PlacementDrive drive, Long academicYearId) {
        return academicYearId == null || (drive.getAcademicYear() != null
                && academicYearId.equals(drive.getAcademicYear().getId()));
    }

    private List<CompanyHiringProjection> hiringAll(Long yearId) {
        return yearId == null ? applications.findCompanyHiringReport(ApplicationStatus.SELECTED)
                : applications.findCompanyHiringReport(ApplicationStatus.SELECTED, yearId);
    }

    private List<CompanyHiringProjection> hiringByStudentDepartment(Long departmentId, Long yearId) {
        return yearId == null ? applications.findCompanyHiringReportByStudentDepartment(ApplicationStatus.SELECTED, departmentId)
                : applications.findCompanyHiringReportByStudentDepartment(ApplicationStatus.SELECTED, departmentId, yearId);
    }

    private List<CompanyHiringProjection> hiringByDriveDepartment(Long departmentId, Long yearId) {
        return yearId == null ? applications.findCompanyHiringReportByDriveDepartment(ApplicationStatus.SELECTED, departmentId)
                : applications.findCompanyHiringReportByDriveDepartment(ApplicationStatus.SELECTED, departmentId, yearId);
    }

    private List<Student> scopedStudents(UserAccount user, Long departmentId) {
        List<Student> source = departmentId == null ? students.findAll() : students.findAllByDepartment_IdOrderByUser_FullNameAsc(departmentId);
        return source.stream().filter(s -> user.getRole() == UserRole.UTPO || (user.getDepartment() != null && s.getDepartment().getId().equals(user.getDepartment().getId()))).toList();
    }

    private List<PlacementApplication> scopedApplications(UserAccount user, Long departmentId, Long academicYearId) {
        return applications.findAll().stream().filter(a -> inScope(user, a.getDrive()) && inYear(a.getDrive(), academicYearId)
                && (departmentId == null || a.getStudent().getDepartment().getId().equals(departmentId))).toList();
    }

    private List<PlacementDrive> scopedDrives(UserAccount user, Long departmentId, Long academicYearId) {
        return drives.findAll().stream().filter(d -> inScope(user, d) && inYear(d, academicYearId)
                && (departmentId == null || (d.getDepartment() != null && d.getDepartment().getId().equals(departmentId)))).toList();
    }

    private String yearLabel(Long academicYearId) {
        if (academicYearId == null) return "All academic years";
        return academicYears.findById(academicYearId).map(AcademicYear::getLabel).orElse("Unknown year");
    }

    private String formatPackageAverage(List<BigDecimal> values) {
        if (values.isEmpty()) return "";
        BigDecimal average = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
        return formatCtc(average, "INR");
    }

    private byte[] render(List<List<String>> rows, boolean excel) {
        if (!excel) return rows.stream().map(row -> row.stream().map(this::csv).reduce((a, b) -> a + "," + b).orElse("") + "\n")
                .reduce(String::concat).orElse("").getBytes(StandardCharsets.UTF_8);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
                entry(zip, "[Content_Types].xml", "<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
                entry(zip, "_rels/.rels", "<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
                entry(zip, "xl/workbook.xml", "<?xml version=\"1.0\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Report\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
                entry(zip, "xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
                StringBuilder sheet = new StringBuilder("<?xml version=\"1.0\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
                for (int r = 0; r < rows.size(); r++) { sheet.append("<row r=\"").append(r + 1).append("\">"); for (int c = 0; c < rows.get(r).size(); c++) sheet.append("<c r=\"").append((char)('A' + c)).append(r + 1).append("\" t=\"inlineStr\"><is><t>").append(xml(rows.get(r).get(c))).append("</t></is></c>"); sheet.append("</row>"); }
                entry(zip, "xl/worksheets/sheet1.xml", sheet.append("</sheetData></worksheet>").toString());
            }
            return out.toByteArray();
        } catch (Exception e) { throw new IllegalStateException("Could not create report", e); }
    }
    private void entry(ZipOutputStream zip, String name, String value) throws Exception { zip.putNextEntry(new ZipEntry(name)); zip.write(value.getBytes(StandardCharsets.UTF_8)); zip.closeEntry(); }
    private String csv(String value) { String v = value == null ? "" : value; return "\"" + v.replace("\"", "\"\"") + "\""; }
    private String xml(String value) { return csv(value).substring(1, csv(value).length() - 1).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    private String value(Object value) { return value == null ? "" : String.valueOf(value); }
    /** Existing records store INR packages in lakhs (12.00 means 12 LPA).
     *  Values at rupee scale are converted defensively for future/imported records. */
    public static String formatCtc(BigDecimal amount, String currency) {
        if (amount == null) return "";
        String unit = currency == null || currency.isBlank() ? "INR" : currency.trim().toUpperCase();
        BigDecimal lpa = amount;
        if ("INR".equals(unit) && amount.abs().compareTo(new BigDecimal("100000")) >= 0)
            lpa = amount.divide(new BigDecimal("100000"), 2, RoundingMode.HALF_UP);
        String formatted = lpa.stripTrailingZeros().toPlainString();
        return "INR".equals(unit) ? formatted + " LPA" : formatted + " " + unit;
    }
    public static String formatDate(java.time.Instant value) { return value == null ? "" : DATE.format(value); }
    private ResponseStatusException forbidden(String message) { return new ResponseStatusException(HttpStatus.FORBIDDEN, message); }
}
