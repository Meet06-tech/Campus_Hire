package com.placementmanagement.api.auth.entity;

import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.UserAccount;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;

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

    @Column(name = "admission_year")
    private Short admissionYear;

    @Column(name = "graduation_year")
    private Short graduationYear;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "academic_program")
    private String academicProgram;

    @Column(name = "current_semester")
    private Short currentSemester;

    @Column(name = "cgpa")
    private BigDecimal cgpa;

    @Column(name = "career_profile")
    private String careerProfile;

    @Column(name = "profile_photo_key", length = 255)
    private String profilePhotoKey;

    @ElementCollection
    @CollectionTable(name = "student_skills", joinColumns = @JoinColumn(name = "student_id"))
    @Column(name = "skill_name", nullable = false, length = 100)
    private Set<String> skills = new LinkedHashSet<>();

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
    public Short getAdmissionYear() { return admissionYear; }
    public Short getGraduationYear() { return graduationYear; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getAcademicProgram() { return academicProgram; }
    public Short getCurrentSemester() { return currentSemester; }
    public BigDecimal getCgpa() { return cgpa; }
    public String getCareerProfile() { return careerProfile; }
    public String getProfilePhotoKey() { return profilePhotoKey; }
    public Set<String> getSkills() { return skills; }

    public void updateProfile(Short admissionYear, Short graduationYear, LocalDate dateOfBirth,
            String phoneNumber, String academicProgram, Short currentSemester,
            BigDecimal cgpa, String careerProfile) {
        updateProfile(admissionYear, graduationYear, dateOfBirth, phoneNumber, academicProgram,
                currentSemester, cgpa, careerProfile, null);
    }

    public void updateProfile(Short admissionYear, Short graduationYear, LocalDate dateOfBirth,
            String phoneNumber, String academicProgram, Short currentSemester,
            BigDecimal cgpa, String careerProfile, Set<String> updatedSkills) {
        this.admissionYear = admissionYear;
        this.graduationYear = graduationYear;
        this.dateOfBirth = dateOfBirth;
        this.phoneNumber = phoneNumber;
        this.academicProgram = academicProgram;
        this.currentSemester = currentSemester;
        this.cgpa = cgpa;
        this.careerProfile = careerProfile;
        if (updatedSkills != null) {
            this.skills.clear();
            this.skills.addAll(updatedSkills);
        }
    }

    public void updateProfilePhotoKey(String profilePhotoKey) {
        this.profilePhotoKey = profilePhotoKey;
    }
}
