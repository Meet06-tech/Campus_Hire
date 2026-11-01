package com.placementmanagement.api.placement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "placement_programs")
public class PlacementProgram {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "program_id") private Long id;
    @Column(name = "program_code", nullable = false, unique = true) private String code;
    @Column(name = "program_name", nullable = false) private String name;
    @Column(name = "is_active", nullable = false) private boolean active;
    @Column(name = "duration_semesters") private Short durationSemesters;
    protected PlacementProgram() {}
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public Short getDurationSemesters() { return durationSemesters; }
}
