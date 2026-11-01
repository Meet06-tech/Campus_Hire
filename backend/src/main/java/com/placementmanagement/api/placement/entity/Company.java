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
    @Column(name = "is_active", nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    protected Company() {}
    public Company(String name, String description, String website, String hrContactName, String hrContactEmail, String hrContactPhone) {
        this.name = name; this.description = description; this.website = website; this.hrContactName = hrContactName;
        this.hrContactEmail = hrContactEmail; this.hrContactPhone = hrContactPhone; this.active = true;
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
}
