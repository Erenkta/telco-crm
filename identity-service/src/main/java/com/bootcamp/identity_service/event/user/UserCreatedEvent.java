package com.bootcamp.identity_service.event.user;

import com.bootcamp.identity_service.entity.enums.ActionEnum;
import com.bootcamp.identity_service.entity.enums.EntityTypeEnum;
import com.bootcamp.identity_service.event.AuditEvent;
import com.bootcamp.identity_service.event.NonCriticalAuditEvent;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserCreatedEvent(
        AuditEvent event
) implements NonCriticalAuditEvent {
    public static UserCreatedEvent build(UUID id) {
        String defaultMessage = String.format("User %s is created",id);
        return new UserCreatedEvent(
                new AuditEvent(EntityTypeEnum.USER, id, ActionEnum.CREATE,  OffsetDateTime.now(),defaultMessage));
    }
    @Override
    public AuditEvent getAuditEvent() {
        return event;
    }
}
