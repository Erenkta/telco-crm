package com.bootcamp.identity_service.event;

import com.bootcamp.identity_service.entity.enums.ActionEnum;
import com.bootcamp.identity_service.entity.enums.EntityTypeEnum;

import java.time.OffsetDateTime;

import java.util.UUID;

// This Audit event will be used for User / Role changes for success scenarios
public record AuditEvent(
        //CHANGED FROM WHERE
        EntityTypeEnum object,
        UUID objectId,
        //DID WHAT
        ActionEnum actionType,
        //WHEN
        OffsetDateTime auditedAt,
        //SHORT DESC
        String desc
) {
}
