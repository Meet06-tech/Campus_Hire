package com.placementmanagement.tools;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.Role;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.repository.DepartmentRepository;
import com.placementmanagement.api.auth.repository.RoleRepository;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.auth.service.StaffEmailAllowlist;

/**
 * Write path used only by the manually invoked local provisioning CLI.
 * It is not a controller, startup runner, or public application service.
 */
@Service
public class StaffProvisioningService {
    private static final long DTPO_DEPARTMENT_ID = 2L;

    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final DepartmentRepository departments;
    private final PasswordEncoder passwordEncoder;
    private final StaffEmailAllowlist staffEmailAllowlist;

    public StaffProvisioningService(UserAccountRepository users, RoleRepository roles,
            DepartmentRepository departments, PasswordEncoder passwordEncoder,
            StaffEmailAllowlist staffEmailAllowlist) {
        this.users = users;
        this.roles = roles;
        this.departments = departments;
        this.passwordEncoder = passwordEncoder;
        this.staffEmailAllowlist = staffEmailAllowlist;
    }

    @Transactional
    public void provision(StaffProvisioningRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Provisioning input is required");
        }
        try {
            String utpoEmail = normalizeEmail(request.utpoEmail(), "U.TPO email");
            String dtpoEmail = normalizeEmail(request.dtpoEmail(), "D.TPO email");
            if (utpoEmail.equals(dtpoEmail)) {
                throw new IllegalArgumentException("The two staff email addresses must be different");
            }
            if (!staffEmailAllowlist.isAllowed(utpoEmail) || !staffEmailAllowlist.isAllowed(dtpoEmail)) {
                throw new IllegalArgumentException("Both staff emails must use an allowed university domain");
            }
            requireText(request.utpoName(), "U.TPO name");
            requireText(request.dtpoName(), "D.TPO name");
            requirePassword(request.utpoPassword(), "U.TPO password");
            requirePassword(request.dtpoPassword(), "D.TPO password");

            if (users.existsByUniversityEmailIgnoreCase(utpoEmail)
                    || users.existsByUniversityEmailIgnoreCase(dtpoEmail)) {
                throw new IllegalStateException("One or both staff email addresses already exist");
            }

            Role utpoRole = roles.findByRoleCode("UTPO")
                    .orElseThrow(() -> new IllegalStateException("UTPO role is not configured"));
            Role dtpoRole = roles.findByRoleCode("DTPO")
                    .orElseThrow(() -> new IllegalStateException("DTPO role is not configured"));
            Department department = departments.findById(DTPO_DEPARTMENT_ID)
                    .filter(Department::isActive)
                    .orElseThrow(() -> new IllegalStateException("Department 2 is missing or inactive"));

            String utpoHash = passwordEncoder.encode(new String(request.utpoPassword()));
            String dtpoHash = passwordEncoder.encode(new String(request.dtpoPassword()));

            UserAccount utpo = new UserAccount(requireText(request.utpoName(), "U.TPO name"),
                    utpoEmail, utpoHash);
            utpo.setRole(utpoRole);

            UserAccount dtpo = new UserAccount(requireText(request.dtpoName(), "D.TPO name"),
                    dtpoEmail, dtpoHash);
            dtpo.setRole(dtpoRole);
            dtpo.setDepartment(department);

            users.save(utpo);
            users.save(dtpo);
        } finally {
            // The encoder accepts CharSequence, so its temporary Strings cannot be explicitly wiped.
            request.close();
        }
    }

    private String normalizeEmail(String value, String field) {
        return requireText(value, field).toLowerCase(Locale.ROOT);
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private void requirePassword(char[] value, String field) {
        if (value == null || value.length == 0) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
