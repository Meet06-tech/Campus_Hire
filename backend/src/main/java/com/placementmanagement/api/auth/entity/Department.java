package com.placementmanagement.api.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "departments")
public class Department {
    @Id
    @Column(name = "department_id")
    private Long id;

    @Column(name = "department_code", nullable = false)
    private String code;

    @Column(name = "department_name", nullable = false)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected Department() {
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}
