package com.placementmanagement.api.placement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.placementmanagement.api.placement.entity.RecruitmentRound;

public interface RecruitmentRoundRepository extends JpaRepository<RecruitmentRound, Long> {
    List<RecruitmentRound> findAllByDrive_IdOrderByOrderAsc(Long driveId);
    Optional<RecruitmentRound> findByIdAndDrive_Id(Long roundId, Long driveId);
    long countByDrive_Id(Long driveId);
    boolean existsByDrive_IdAndFinalRoundTrue(Long driveId);
}
