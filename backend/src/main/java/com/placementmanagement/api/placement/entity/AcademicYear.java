package com.placementmanagement.api.placement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "academic_years")
public class AcademicYear {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "academic_year_id") private Long id;
    @Column(name = "year_label", nullable = false) private String label;
    @Column(name = "status", nullable = false) private String status;
    protected AcademicYear() {}
    public Long getId() { return id; }
    public String getLabel() { return label; }
    public String getStatus() { return status; }
}
