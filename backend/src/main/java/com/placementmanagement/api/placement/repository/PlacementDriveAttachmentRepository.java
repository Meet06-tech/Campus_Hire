package com.placementmanagement.api.placement.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.placementmanagement.api.placement.entity.PlacementDriveAttachment;

public interface PlacementDriveAttachmentRepository extends JpaRepository<PlacementDriveAttachment, Long> {
    List<PlacementDriveAttachment> findAllByDrive_IdOrderByCreatedAtAsc(Long driveId);
}
