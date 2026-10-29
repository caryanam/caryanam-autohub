package com.autohub.repository;

import com.autohub.entity.Inspector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InspectorRepository extends JpaRepository<Inspector, Long> {

    Optional<Inspector> findByEmail(String email);

    Optional<Inspector> findByMobile(String mobile);

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobile);

    List<Inspector> findByAssignedAreaIgnoreCase(String assignedArea);

    List<Inspector> findByIsActiveTrue();

    long countByIsActiveTrue();
}

