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
import com.placementmanagement.api.placement.repository.PlacementApplicationRepository;
import com.placementmanagement.api.placement.repository.PlacementDriveRepository;
import com.placementmanagement.api.placement.repository.PlacementApplicationRepository.CompanyHiringProjection;

@Service
public class PlacementReportService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);
    private final PlacementAuthorizationService authorization;
    private final PlacementApplicationRepository applications;
    private final PlacementDriveRepository drives;
    private final StudentRepository students;

    public PlacementReportService(PlacementAuthorizationService authorization,
            PlacementApplicationRepository applications, PlacementDriveRepository drives, StudentRepository students) {
        this.authorization = authorization;
        this.applications = applications;
        this.drives = drives;
        this.students = students;
    }

    public byte[] placed(Authentication auth, Long departmentId, boolean excel) {
        UserAccount user = authorization.staff(auth);
        departmentId = scopedDepartment(user, departmentId);
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Student name", "Enrollment number", "Email", "Department", "Program", "Semester",
                "Company", "Drive", "Status", "CTC", "Applied at", "Placement date"));
        placedApplications(departmentId).stream()
                .filter(a -> inScope(user, a.getDrive())).forEach(a -> rows.add(placedRow(a)));
        return render(rows, excel);
    }

    public byte[] visitedCompanies(Authentication auth, Long departmentId, boolean excel) {
        UserAccount user = authorization.staff(auth);
        departmentId = scopedDepartment(user, departmentId);
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Company", "Website", "Contact email", "Drive", "Drive status", "Deadline", "Employment type", "CTC", "Stipend"));
        visitedDrives(user, departmentId).forEach(d -> rows.add(List.of(
                d.getCompany().getName(), value(d.getCompany().getWebsite()),
                value(d.getCompany().getHrContactEmail()), value(d.getTitle()), d.getStatus().name(),
                formatDate(d.getApplicationDeadline()), value(d.getEmploymentType()),
                formatCtc(d.getCtcAmount(), d.getCtcCurrency()), value(d.getStipend()))));
        return render(rows, excel);
    }

    public byte[] companyHiring(Authentication auth, Long departmentId, boolean excel) {
        UserAccount user = authorization.staff(auth);
        departmentId = scopedDepartment(user, departmentId);
        List<CompanyHiringProjection> grouped = user.getRole() == UserRole.DTPO
                ? applications.findCompanyHiringReportByDriveDepartment(ApplicationStatus.SELECTED, departmentId)
                : departmentId == null
                    ? applications.findCompanyHiringReport(ApplicationStatus.SELECTED)
                    : applications.findCompanyHiringReportByStudentDepartment(ApplicationStatus.SELECTED, departmentId);
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
        UserAccount user = authorization.staff(auth);
        if (user.getRole() == UserRole.DTPO && !Objects.equals(user.getDepartment().getId(), departmentId == null ? user.getDepartment().getId() : departmentId))
            throw forbidden("DTPO cannot export another department");
        List<Student> source = departmentId == null ? students.findAll() : students.findAllByDepartment_IdOrderByUser_FullNameAsc(departmentId);
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Student name", "Enrollment number", "Email", "Phone", "Department", "Program", "Semester", "Graduation year", "CGPA"));
        source.stream().filter(s -> departmentId == null || s.getDepartment().getId().equals(departmentId))
                .filter(s -> user.getRole() == UserRole.UTPO || s.getDepartment().getId().equals(user.getDepartment().getId()))
                .forEach(s -> rows.add(List.of(s.getUser().getFullName(), s.getEnrollmentNumber(), s.getUser().getUniversityEmail(),
                        value(s.getPhoneNumber()), s.getDepartment().getName(), value(s.getAcademicProgram()), value(s.getCurrentSemester()),
                        value(s.getGraduationYear()), value(s.getCgpa()))));
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
