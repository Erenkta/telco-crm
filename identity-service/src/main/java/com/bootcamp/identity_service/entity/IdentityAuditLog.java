package com.bootcamp.identity_service.entity;

import com.bootcamp.identity_service.entity.enums.ActionEnum;
import com.bootcamp.identity_service.entity.enums.EntityTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table(name = "identity_audit_logs")
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class IdentityAuditLog {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private EntityTypeEnum entityType;

    private UUID entityId;

    @Enumerated(EnumType.STRING)
    private ActionEnum action;

    private String detail;

    private String performedBy;
    private OffsetDateTime createdAt;
}
