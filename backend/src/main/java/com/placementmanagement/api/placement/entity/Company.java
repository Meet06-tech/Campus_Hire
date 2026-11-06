package com.placementmanagement.api.placement.entity;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "companies")
public class Company {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id") private Long id;
    @Column(name = "company_name", nullable = false) private String name;
    @Column(name = "profile") private String description;
    @Column(name = "website_url") private String website;
    @Column(name = "contact_name") private String hrContactName;
    @Column(name = "contact_email") private String hrContactEmail;
    @Column(name = "contact_phone") private String hrContactPhone;
    @Column(name = "industry") private String industry;
    @Column(name = "employment_type") private String employmentType;
    @Column(name = "position_type") private String positionType;
    @Column(name = "default_location") private String defaultLocation;
    @Column(name = "ctc_amount") private java.math.BigDecimal ctcAmount;
    @Column(name = "ctc_currency") private String ctcCurrency;
    @Column(name = "stipend_type") private String stipendType;
    @Column(name = "stipend_min_amount") private java.math.BigDecimal stipendMinAmount;
    @Column(name = "stipend_max_amount") private java.math.BigDecimal stipendMaxAmount;
    @Column(name = "stipend_currency") private String stipendCurrency;
    @Column(name = "stipend_payment_period") private String stipendPaymentPeriod;
    @Column(name = "other_benefits") private String otherBenefits;
    @Column(name = "bond_details") private String bondDetails;
    @Column(name = "stipend") private String stipend;
    @Column(name = "is_active", nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    protected Company() {}
    public Company(String name, String description, String website, String hrContactName, String hrContactEmail, String hrContactPhone) {
        this.name = name; this.description = description; this.website = website; this.hrContactName = hrContactName;
        this.hrContactEmail = hrContactEmail; this.hrContactPhone = hrContactPhone; this.active = true;
    }
    public void updateOptionalDetails(String employmentType, String positionType, String defaultLocation, java.math.BigDecimal ctcAmount,
            String ctcCurrency, String stipendType, java.math.BigDecimal stipendMinAmount, java.math.BigDecimal stipendMaxAmount,
            String stipendCurrency, String stipendPaymentPeriod, String otherBenefits, String bondDetails) {
        this.employmentType = employmentType; this.positionType = positionType; this.defaultLocation = defaultLocation; this.ctcAmount = ctcAmount;
        this.ctcCurrency = ctcCurrency; this.stipendType = stipendType; this.stipendMinAmount = stipendMinAmount; this.stipendMaxAmount = stipendMaxAmount;
        this.stipendCurrency = stipendCurrency; this.stipendPaymentPeriod = stipendPaymentPeriod; this.otherBenefits = otherBenefits; this.bondDetails = bondDetails;
        this.updatedAt = Instant.now();
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getWebsite() { return website; }
    public String getHrContactName() { return hrContactName; }
    public String getHrContactEmail() { return hrContactEmail; }
    public String getHrContactPhone() { return hrContactPhone; }
    public String getIndustry() { return industry; }
    public boolean isActive() { return active; }
    public String getEmploymentType() { return employmentType; }
    public String getPositionType() { return positionType; }
    public String getDefaultLocation() { return defaultLocation; }
    public java.math.BigDecimal getCtcAmount() { return ctcAmount; }
    public String getCtcCurrency() { return ctcCurrency; }
    public String getStipendType() { return stipendType; }
    public java.math.BigDecimal getStipendMinAmount() { return stipendMinAmount; }
    public java.math.BigDecimal getStipendMaxAmount() { return stipendMaxAmount; }
    public String getStipendCurrency() { return stipendCurrency; }
    public String getStipendPaymentPeriod() { return stipendPaymentPeriod; }
    public String getOtherBenefits() { return otherBenefits; }
    public String getBondDetails() { return bondDetails; }
    public String getStipend() { return stipend; }
    public void setStipend(String stipend) { this.stipend = stipend; this.updatedAt = Instant.now(); }
}
