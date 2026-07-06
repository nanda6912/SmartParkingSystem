package com.smartparking.repository;

import com.smartparking.entity.StaffUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repository interface for StaffUser database operations.
 */
@Repository
public interface StaffUserRepository extends JpaRepository<StaffUser, Long> {

    Optional<StaffUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
