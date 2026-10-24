package com.bootcamp.identity_service.event.user;

import com.bootcamp.identity_service.entity.enums.ActionEnum;
import com.bootcamp.identity_service.entity.enums.EntityTypeEnum;
import com.bootcamp.identity_service.event.AuditEvent;
import com.bootcamp.identity_service.event.NonCriticalAuditEvent;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserModifiedEvent(
        AuditEvent event
) implements NonCriticalAuditEvent {
    public static UserModifiedEvent build(String desc,UUID id) {
        return new UserModifiedEvent(
                new AuditEvent(EntityTypeEnum.USER, id,ActionEnum.UPDATE,  OffsetDateTime.now(),desc));
    }
    public static UserModifiedEvent build(UUID id) {
        String defaultMessage = String.format("User %s is modified", id);
        return new UserModifiedEvent(
                new AuditEvent(EntityTypeEnum.USER, id, ActionEnum.UPDATE,  OffsetDateTime.now(),defaultMessage));
    }
    @Override
    public AuditEvent getAuditEvent() {
        return event;
    }

}
