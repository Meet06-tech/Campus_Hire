package com.placementmanagement.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import jakarta.mail.internet.MimeMessage;

import com.placementmanagement.api.PlacementManagementApplication;
import com.placementmanagement.api.auth.dto.AuthDtos.RegisterRequest;
import com.placementmanagement.api.auth.dto.RegistrationDtos.RegistrationOtpRequest;
import com.placementmanagement.api.auth.entity.Department;
import com.placementmanagement.api.auth.entity.PendingStudentRegistration;
import com.placementmanagement.api.auth.repository.DepartmentRepository;
import com.placementmanagement.api.auth.repository.PendingStudentRegistrationRepository;
import com.placementmanagement.api.auth.repository.UserAccountRepository;
import com.placementmanagement.api.auth.service.RegistrationService;

@SpringBootTest(classes = PlacementManagementApplication.class)
@ActiveProfiles("integration-test")
@Import({IntegrationDatabaseGuard.class, RegistrationPostgresIntegrationTest.MailTestConfiguration.class})
class RegistrationPostgresIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired RegistrationService registrations;
    @Autowired PendingStudentRegistrationRepository pending;
    @Autowired UserAccountRepository users;
    @Autowired DepartmentRepository departments;
    @Autowired PasswordEncoder encoder;
    @Autowired TestMailSender mailSender;

    private String email;
    private String enrollment;
    private Long departmentId;

    @BeforeEach
    void prepareReferenceData() {
        String unique = Long.toString(Math.abs(System.nanoTime()));
        email = "25mca" + unique + "@charusat.edu.in";
        enrollment = "OTP-" + unique;
        jdbc.update("insert into roles(role_code, role_name) values ('STUDENT', 'Integration Student') on conflict (role_code) do nothing");
        departmentId = jdbc.query("select department_id from departments where department_code = 'TESTOTP'",
                result -> result.next() ? result.getLong(1) : null);
        if (departmentId == null) {
            departmentId = jdbc.queryForObject("insert into departments(department_code, department_name, is_active, created_at, updated_at) values ('TESTOTP', 'OTP Test Department', true, current_timestamp, current_timestamp) returning department_id", Long.class);
        }
        jdbc.update("insert into department_email_codes(department_id, email_code, is_active, created_at, updated_at) values (?, 'MCA', true, current_timestamp, current_timestamp) on conflict (email_code) do update set department_id = excluded.department_id, is_active = true", departmentId);
        mailSender.messages.clear();
    }

    @AfterEach
    void cleanRegistrationData() {
        jdbc.update("delete from pending_student_registrations where lower(university_email) = lower(?)", email);
        jdbc.update("delete from students where user_id in (select user_id from users where lower(university_email) = lower(?))", email);
        jdbc.update("delete from users where lower(university_email) = lower(?)", email);
    }

    @Test
    void registrationRequiresOtpAndCreatesAccountExactlyOnceAfterVerification() {
        whenMailIsAccepted();
        var start = registrations.begin(new RegisterRequest("OTP Student", enrollment, email, "safe-password", "safe-password"));

        assertEquals(0, jdbc.queryForObject("select count(*) from users where university_email = ?", Integer.class, email));
        assertEquals(1, jdbc.queryForObject("select count(*) from pending_student_registrations where university_email = ?", Integer.class, email));
        assertEquals(1, mailSender.messages.size());
        String otp = mailSender.messages.get(0).getText().replaceAll(".*?(\\d{6}).*", "$1");

        registrations.verify(new RegistrationOtpRequest(start.registrationId(), otp));
        assertEquals(1, jdbc.queryForObject("select count(*) from users where university_email = ?", Integer.class, email));
        assertEquals(1, jdbc.queryForObject("select count(*) from students where enrollment_number = ?", Integer.class, enrollment));
        assertTrue(encoder.matches("safe-password", users.findByUniversityEmailIgnoreCase(email).orElseThrow().getPasswordHash()));
        assertNotNull(pending.findById(start.registrationId()).orElseThrow().getConsumedAt());
        assertThrows(ResponseStatusException.class, () -> registrations.verify(new RegistrationOtpRequest(start.registrationId(), otp)));
    }

    @Test
    void duplicateRegistrationIsRejectedAfterTheFirstAccountExists() {
        whenMailIsAccepted();
        var request = new RegisterRequest("OTP Student", enrollment, email, "safe-password", "safe-password");
        registrations.begin(request);
        assertThrows(ResponseStatusException.class, () -> registrations.begin(request));
    }

    @Test
    void expiredRegistrationOtpIsRejected() {
        Department department = departments.findById(departmentId).orElseThrow();
        PendingStudentRegistration record = pending.saveAndFlush(new PendingStudentRegistration(
                "Expired Student", enrollment, email, encoder.encode("safe-password"), department,
                encoder.encode("123456"), Instant.now().minusSeconds(1)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> registrations.verify(new RegistrationOtpRequest(record.getId(), "123456")));
        assertEquals(400, error.getStatusCode().value());
        assertEquals(0, jdbc.queryForObject("select count(*) from users where university_email = ?", Integer.class, email));
    }

    private void whenMailIsAccepted() {
        // The in-memory sender accepts the message without contacting SMTP.
    }

    @TestConfiguration
    static class MailTestConfiguration {
        @Bean
        TestMailSender testMailSender() {
            return new TestMailSender();
        }
    }

    static class TestMailSender implements JavaMailSender {
        final List<SimpleMailMessage> messages = new ArrayList<>();

        @Override
        public void send(SimpleMailMessage message) {
            messages.add(message);
        }

        @Override
        public void send(SimpleMailMessage... messages) {
            for (SimpleMailMessage message : messages) send(message);
        }

        @Override
        public MimeMessage createMimeMessage() {
            throw new UnsupportedOperationException("SMTP is not used by this integration test");
        }

        @Override
        public MimeMessage createMimeMessage(java.io.InputStream contentStream) throws MailException {
            throw new UnsupportedOperationException("SMTP is not used by this integration test");
        }

        @Override
        public void send(MimeMessage message) {
            throw new UnsupportedOperationException("SMTP is not used by this integration test");
        }

        @Override
        public void send(MimeMessage... messages) {
            throw new UnsupportedOperationException("SMTP is not used by this integration test");
        }
    }
}
