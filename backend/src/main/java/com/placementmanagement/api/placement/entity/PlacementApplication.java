package com.placementmanagement.api.placement.entity;

import java.time.Instant;
import jakarta.persistence.*;
import com.placementmanagement.api.auth.entity.Student;

@Entity
@Table(name = "placement_applications", uniqueConstraints = @UniqueConstraint(name = "placement_applications_drive_student_uq", columnNames = {"drive_id", "student_id"}))
public class PlacementApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id") private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "drive_id") private PlacementDrive drive;
    @ManyToOne(optional = false) @JoinColumn(name = "student_id") private Student student;
    @Column(name = "applied_at", nullable = false, updatable = false) private Instant appliedAt = Instant.now();
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ApplicationStatus status = ApplicationStatus.APPLIED;
    protected PlacementApplication() {}
    public PlacementApplication(PlacementDrive drive, Student student) { this.drive = drive; this.student = student; }
    public Long getId() { return id; }
    public PlacementDrive getDrive() { return drive; }
    public Student getStudent() { return student; }
    public Instant getAppliedAt() { return appliedAt; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
}
