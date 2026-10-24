package com.bootcamp.identity_service.repository;

import com.bootcamp.identity_service.entity.IdentityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IdentityAuditLogRepository extends JpaRepository<IdentityAuditLog, UUID> {
}
