package com.placementmanagement.api.auth.entity;

import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.UserAccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_id")
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAccount user;

    @Column(name = "enrollment_number", nullable = false, unique = true, length = 80)
    private String enrollmentNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    protected Student() {
    }

    public Student(UserAccount user, String enrollmentNumber, Department department) {
        this.user = user;
        this.enrollmentNumber = enrollmentNumber;
        this.department = department;
        user.setStudent(this);
    }

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public String getEnrollmentNumber() { return enrollmentNumber; }
    public Department getDepartment() { return department; }
}
