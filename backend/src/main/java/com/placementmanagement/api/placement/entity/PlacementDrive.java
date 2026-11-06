package com.placementmanagement.api.placement.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import jakarta.persistence.*;
import com.placementmanagement.api.auth.entity.*;

@Entity
@Table(name = "placement_drives")
public class PlacementDrive {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "drive_id") private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "academic_year_id") private AcademicYear academicYear;
    @ManyToOne(optional = false) @JoinColumn(name = "company_id") private Company company;
    @ManyToOne @JoinColumn(name = "department_id") private Department department;
    @ManyToOne(optional = false) @JoinColumn(name = "created_by_user_id") private UserAccount createdBy;
    @Column(name = "job_title", nullable = false) private String title;
    @Column(name = "job_description") private String jobDescription;
    @Column(name = "eligibility_criteria") private String eligibilityCriteria;
    @Column(name = "compensation_package") private String packageSalary;
    @Column(name = "location") private String location;
    @Column(name = "work_mode") private String jobType;
    @Column(name = "application_opens_at", nullable = false) private Instant applicationOpensAt;
    @Column(name = "application_closes_at", nullable = false) private Instant applicationDeadline;
    @Column(name = "scheduled_at") private Instant scheduledAt;
    @Column(name = "instructions") private String instructions;
    @Column(name = "employment_type") private String employmentType;
    @Column(name = "position_type") private String positionType;
    @Column(name = "ctc_amount") private BigDecimal ctcAmount;
    @Column(name = "ctc_currency") private String ctcCurrency;
    @Column(name = "stipend_type") private String stipendType;
    @Column(name = "stipend_min_amount") private BigDecimal stipendMinAmount;
    @Column(name = "stipend_max_amount") private BigDecimal stipendMaxAmount;
    @Column(name = "stipend_currency") private String stipendCurrency;
    @Column(name = "stipend_payment_period") private String stipendPaymentPeriod;
    @Column(name = "other_benefits") private String otherBenefits;
    @Column(name = "bond_details") private String bondDetails;
    @Column(name = "stipend") private String stipend;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private DriveStatus status;
    @Column(name = "minimum_cgpa") private BigDecimal minimumCgpa;
    @Column(name = "graduation_year") private Short graduationYear;
    @Column(name = "maximum_active_backlogs") private Integer maximumActiveBacklogs;
    @ManyToMany
    @JoinTable(name = "placement_drive_programs", joinColumns = @JoinColumn(name = "drive_id"), inverseJoinColumns = @JoinColumn(name = "program_id"))
    private Set<PlacementProgram> programs = new LinkedHashSet<>();
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    @Column(name = "operationally_archived", nullable = false) private boolean operationallyArchived;
    @Column(name = "archived_at") private Instant archivedAt;
    protected PlacementDrive() {}
    public PlacementDrive(AcademicYear academicYear, Company company, Department department, UserAccount createdBy, String title, String jobDescription,
            String jobType, String location, String packageSalary, Instant opensAt, Instant deadline, BigDecimal minimumCgpa,
            Short graduationYear, Integer maximumActiveBacklogs, Set<PlacementProgram> programs) {
        this.academicYear = academicYear; this.company = company; this.department = department; this.createdBy = createdBy; this.title = title;
        this.jobDescription = jobDescription; this.jobType = jobType; this.location = location; this.packageSalary = packageSalary;
        this.applicationOpensAt = opensAt; this.applicationDeadline = deadline; this.status = DriveStatus.DRAFT; this.minimumCgpa = minimumCgpa;
        this.graduationYear = graduationYear; this.maximumActiveBacklogs = maximumActiveBacklogs;
        if (programs != null) this.programs = programs;
    }
    public Long getId() { return id; }
    public Company getCompany() { return company; }
    public AcademicYear getAcademicYear() { return academicYear; }
    public Department getDepartment() { return department; }
    public String getTitle() { return title; }
    public String getJobDescription() { return jobDescription; }
    public String getJobType() { return jobType; }
    public String getLocation() { return location; }
    public String getPackageSalary() { return packageSalary; }
    public Instant getApplicationDeadline() { return applicationDeadline; }
    public Instant getApplicationOpensAt() { return applicationOpensAt; }
    public DriveStatus getStatus() { return status; }
    public BigDecimal getMinimumCgpa() { return minimumCgpa; }
    public Short getGraduationYear() { return graduationYear; }
    public Integer getMaximumActiveBacklogs() { return maximumActiveBacklogs; }
    public String getInstructions() { return instructions; }
    public String getEmploymentType() { return employmentType; }
    public String getPositionType() { return positionType; }
    public BigDecimal getCtcAmount() { return ctcAmount; }
    public String getCtcCurrency() { return ctcCurrency; }
    public String getStipendType() { return stipendType; }
    public BigDecimal getStipendMinAmount() { return stipendMinAmount; }
    public BigDecimal getStipendMaxAmount() { return stipendMaxAmount; }
    public String getStipendCurrency() { return stipendCurrency; }
    public String getStipendPaymentPeriod() { return stipendPaymentPeriod; }
    public String getOtherBenefits() { return otherBenefits; }
    public String getBondDetails() { return bondDetails; }
    public String getStipend() { return stipend; }
    public void updateOptionalDetails(String employmentType, String positionType, BigDecimal ctcAmount, String ctcCurrency,
            String stipendType, BigDecimal stipendMinAmount, BigDecimal stipendMaxAmount, String stipendCurrency,
            String stipendPaymentPeriod, String otherBenefits, String bondDetails) {
        this.employmentType = employmentType; this.positionType = positionType; this.ctcAmount = ctcAmount; this.ctcCurrency = ctcCurrency;
        this.stipendType = stipendType; this.stipendMinAmount = stipendMinAmount; this.stipendMaxAmount = stipendMaxAmount;
        this.stipendCurrency = stipendCurrency; this.stipendPaymentPeriod = stipendPaymentPeriod;
        this.otherBenefits = otherBenefits; this.bondDetails = bondDetails;
    }
    public Set<PlacementProgram> getPrograms() { return programs; }
    public void setStatus(DriveStatus status) { this.status = status; }
    public void setStipend(String stipend) { this.stipend = stipend; }
    public boolean isOperationallyArchived() { return operationallyArchived; }
    public Instant getArchivedAt() { return archivedAt; }
    public void archive() { this.operationallyArchived = true; this.archivedAt = Instant.now(); this.updatedAt = Instant.now(); }
}
