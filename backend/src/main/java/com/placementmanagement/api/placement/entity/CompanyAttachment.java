package com.placementmanagement.api.placement.entity;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "company_attachments")
public class CompanyAttachment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id") private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "company_id") private Company company;
    @Column(name = "original_file_name", nullable = false) private String originalFileName;
    @Column(name = "storage_key", nullable = false, unique = true) private String storageKey;
    @Column(name = "content_type", nullable = false) private String contentType;
    @Column(name = "size_bytes", nullable = false) private long sizeBytes;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    protected CompanyAttachment() {}
    public CompanyAttachment(Company company, String originalFileName, String storageKey, String contentType, long sizeBytes) {
        this.company = company; this.originalFileName = originalFileName; this.storageKey = storageKey; this.contentType = contentType; this.sizeBytes = sizeBytes;
    }
    public Long getId() { return id; }
    public Company getCompany() { return company; }
    public String getOriginalFileName() { return originalFileName; }
    public String getStorageKey() { return storageKey; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
}
