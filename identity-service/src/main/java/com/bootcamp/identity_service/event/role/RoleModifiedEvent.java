package com.bootcamp.identity_service.event.role;

import com.bootcamp.identity_service.entity.enums.ActionEnum;
import com.bootcamp.identity_service.entity.enums.EntityTypeEnum;
import com.bootcamp.identity_service.event.AuditEvent;
import com.bootcamp.identity_service.event.NonCriticalAuditEvent;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RoleModifiedEvent(
        AuditEvent event
) implements NonCriticalAuditEvent  {
    public static RoleModifiedEvent build(UUID id) {
        String defaultMessage = String.format("Role %s's fields are updated",id);
        return new RoleModifiedEvent(
                new AuditEvent(EntityTypeEnum.ROLE,id, ActionEnum.UPDATE,  OffsetDateTime.now(),defaultMessage));
    }
    @Override
    public AuditEvent getAuditEvent() {
        return event;
    }
}
