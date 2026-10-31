package com.placementmanagement.api.placement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.placementmanagement.api.placement.entity.RoundResult;

public interface RoundResultRepository extends JpaRepository<RoundResult, Long> {
    List<RoundResult> findAllByRound_Id(Long roundId);
    List<RoundResult> findAllByApplication_IdOrderByRound_OrderAsc(Long applicationId);
    Optional<RoundResult> findByRound_IdAndApplication_Id(Long roundId, Long applicationId);
    boolean existsByRound_IdAndApplication_Id(Long roundId, Long applicationId);
    boolean existsByRound_Drive_IdAndApplication_IdAndRound_OrderGreaterThan(Long driveId, Long applicationId, Integer order);
}
