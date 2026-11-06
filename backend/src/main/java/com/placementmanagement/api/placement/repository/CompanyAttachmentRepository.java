package com.placementmanagement.api.placement.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.placementmanagement.api.placement.entity.CompanyAttachment;

public interface CompanyAttachmentRepository extends JpaRepository<CompanyAttachment, Long> {
    List<CompanyAttachment> findAllByCompany_IdOrderByCreatedAtAsc(Long companyId);
}
