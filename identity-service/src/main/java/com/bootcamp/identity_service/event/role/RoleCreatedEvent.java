package com.bootcamp.identity_service.event.role;

import com.bootcamp.identity_service.entity.enums.ActionEnum;
import com.bootcamp.identity_service.entity.enums.EntityTypeEnum;
import com.bootcamp.identity_service.event.AuditEvent;
import com.bootcamp.identity_service.event.NonCriticalAuditEvent;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RoleCreatedEvent(
        AuditEvent event
) implements NonCriticalAuditEvent {
    public static RoleCreatedEvent build(UUID id) {
        String defaultMessage = String.format("Role created with id %s", id);
        return new RoleCreatedEvent(
                new AuditEvent(EntityTypeEnum.ROLE, id, ActionEnum.CREATE,  OffsetDateTime.now(),defaultMessage));
    }

    @Override
    public AuditEvent getAuditEvent() {
        return event;
    }
}
