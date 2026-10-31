package com.placementmanagement.api.placement.repository;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import com.placementmanagement.api.placement.entity.PlacementProgram;
public interface PlacementProgramRepository extends JpaRepository<PlacementProgram, Long> {
    List<PlacementProgram> findAllByIdIn(Collection<Long> ids);
    Optional<PlacementProgram> findByCodeIgnoreCase(String code);
}
