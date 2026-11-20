package com.placementmanagement.api.placement;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import com.placementmanagement.api.auth.entity.Student;
import com.placementmanagement.api.auth.entity.UserAccount;
import com.placementmanagement.api.auth.entity.UserRole;
import com.placementmanagement.api.placement.dto.PlacementDtos.CompanyResponse;
import com.placementmanagement.api.placement.dto.PlacementDtos.DriveResponse;
import com.placementmanagement.api.placement.entity.Company;
import com.placementmanagement.api.placement.entity.CompanyAttachment;
import com.placementmanagement.api.placement.entity.PlacementDrive;
import com.placementmanagement.api.placement.entity.PlacementDriveAttachment;
import com.placementmanagement.api.placement.repository.CompanyAttachmentRepository;
import com.placementmanagement.api.placement.repository.CompanyRepository;
import com.placementmanagement.api.placement.repository.PlacementDriveAttachmentRepository;
import com.placementmanagement.api.placement.repository.PlacementDriveRepository;
import com.placementmanagement.api.placement.service.CompanyAttachmentService;
import com.placementmanagement.api.placement.service.PlacementAttachmentService;
import com.placementmanagement.api.placement.service.PlacementAttachmentStorageService;
import com.placementmanagement.api.placement.service.PlacementAuthorizationService;
import com.placementmanagement.api.placement.service.PlacementService;

class AttachmentAccessTest {
    @Test
    void studentDownloadsOnlyCompanyAttachmentForVisibleDrive() {
        var companies = mock(CompanyRepository.class);
        var attachments = mock(CompanyAttachmentRepository.class);
        var authorization = mock(PlacementAuthorizationService.class);
        var storage = mock(PlacementAttachmentStorageService.class);
        var placement = mock(PlacementService.class);
        var auth = mock(Authentication.class);
        var company = mock(Company.class);
        var attachment = mock(CompanyAttachment.class);
        var visibleDrive = mock(DriveResponse.class);
        var visibleCompany = mock(CompanyResponse.class);
        when(companies.findById(4L)).thenReturn(Optional.of(company));
        when(authorization.staff(auth)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        when(authorization.student(auth)).thenReturn(mock(Student.class));
        when(placement.listStudentDrives(auth)).thenReturn(List.of(visibleDrive));
        when(visibleDrive.company()).thenReturn(visibleCompany);
        when(visibleCompany.id()).thenReturn(4L);
        when(attachments.findById(8L)).thenReturn(Optional.of(attachment));
        when(attachment.getCompany()).thenReturn(company);
        when(company.getId()).thenReturn(4L);
        when(attachment.getStorageKey()).thenReturn("file-key");
        when(storage.read("file-key")).thenReturn(new byte[] {1, 2});

        var service = new CompanyAttachmentService(companies, attachments, authorization, storage, placement);
        assertArrayEquals(new byte[] {1, 2}, service.download(auth, 4L, 8L).bytes());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.download(auth, 4L, 9L)).getStatusCode().value());
    }

    @Test
    void studentCannotReadCompanyWithoutVisibleDrive() {
        var companies = mock(CompanyRepository.class);
        var authorization = mock(PlacementAuthorizationService.class);
        var placement = mock(PlacementService.class);
        var auth = mock(Authentication.class);
        when(companies.findById(4L)).thenReturn(Optional.of(mock(Company.class)));
        when(authorization.staff(auth)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        when(authorization.student(auth)).thenReturn(mock(Student.class));
        when(placement.listStudentDrives(auth)).thenReturn(List.of());
        var service = new CompanyAttachmentService(companies, mock(CompanyAttachmentRepository.class),
                authorization, mock(PlacementAttachmentStorageService.class), placement);
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.download(auth, 4L, 8L)).getStatusCode().value());
    }

    @Test
    void driveDownloadRejectsAttachmentBelongingToAnotherDrive() {
        var attachments = mock(PlacementDriveAttachmentRepository.class);
        var attachment = mock(PlacementDriveAttachment.class);
        var drive = mock(PlacementDrive.class);
        var auth = mock(Authentication.class);
        var drives = mock(PlacementDriveRepository.class);
        var authorization = mock(PlacementAuthorizationService.class);
        var staff = mock(UserAccount.class);
        when(drives.findById(4L)).thenReturn(Optional.of(mock(PlacementDrive.class)));
        when(authorization.staff(auth)).thenReturn(staff);
        when(staff.getRole()).thenReturn(UserRole.UTPO);
        when(attachments.findById(8L)).thenReturn(Optional.of(attachment));
        when(attachment.getDrive()).thenReturn(drive);
        when(drive.getId()).thenReturn(99L);
        var service = new PlacementAttachmentService(drives, attachments,
                authorization, mock(PlacementAttachmentStorageService.class),
                mock(PlacementService.class));
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.download(auth, 4L, 8L)).getStatusCode().value());
    }
}
