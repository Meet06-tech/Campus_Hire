package com.placementmanagement.api.placement.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.placementmanagement.api.placement.entity.Company;
public interface CompanyRepository extends JpaRepository<Company, Long> {}
