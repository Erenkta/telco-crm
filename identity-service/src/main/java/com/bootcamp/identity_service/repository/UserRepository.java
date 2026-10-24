package com.bootcamp.identity_service.repository;

import com.bootcamp.identity_service.entity.User;
import com.bootcamp.identity_service.entity.enums.StatusEnum;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Page<User> findAllByStatusNot(StatusEnum status, Pageable pageable);

    Optional<User> findByIdAndStatusNot(UUID id,StatusEnum status);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select u from User u where u.id = :id and u.status <> 'DELETED'")
    Optional<User> findActiveByIdForUpdate(@Param("id") UUID id);
}