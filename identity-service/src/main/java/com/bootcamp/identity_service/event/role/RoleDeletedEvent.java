package com.bootcamp.identity_service.event.role;

import com.bootcamp.identity_service.entity.enums.ActionEnum;
import com.bootcamp.identity_service.entity.enums.EntityTypeEnum;
import com.bootcamp.identity_service.event.AuditEvent;
import com.bootcamp.identity_service.event.CriticalAuditEvent;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RoleDeletedEvent(
        AuditEvent event
) implements CriticalAuditEvent {
    public static RoleDeletedEvent build(String desc, UUID id) {
        return new RoleDeletedEvent(
                new AuditEvent(EntityTypeEnum.ROLE,id, ActionEnum.DELETE,  OffsetDateTime.now(),desc));
    }
    public static RoleDeletedEvent build(UUID id) {
        String defaultMessage = String.format("Role %s is deleted",id);
        return new RoleDeletedEvent(
                new AuditEvent(EntityTypeEnum.ROLE,id,ActionEnum.DELETE, OffsetDateTime.now(),defaultMessage));
    }
    @Override
    public AuditEvent getAuditEvent() {
        return event;
    }
}
