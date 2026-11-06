package com.placementmanagement.api.announcement.entity;

import java.time.Instant;

import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.UserAccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "announcements")
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "announcement_id")
    private Long id;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @ManyToOne(optional = false)
    @JoinColumn(name = "publisher_user_id", nullable = false)
    private UserAccount publisher;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "published_at", nullable = false, updatable = false)
    private Instant publishedAt = Instant.now();

    protected Announcement() {}

    public Announcement(String subject, String title, String message, UserAccount publisher, Department department) {
        this.subject = subject;
        this.title = title;
        this.message = message;
        this.publisher = publisher;
        this.department = department;
    }

    public Long getId() { return id; }
    public String getSubject() { return subject; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public UserAccount getPublisher() { return publisher; }
    public Department getDepartment() { return department; }
    public Instant getPublishedAt() { return publishedAt; }
}
