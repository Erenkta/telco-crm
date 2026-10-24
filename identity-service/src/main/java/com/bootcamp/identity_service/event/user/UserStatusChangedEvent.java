package com.bootcamp.identity_service.event.user;

import com.bootcamp.identity_service.entity.enums.ActionEnum;
import com.bootcamp.identity_service.entity.enums.EntityTypeEnum;
import com.bootcamp.identity_service.event.AuditEvent;
import com.bootcamp.identity_service.event.CriticalAuditEvent;
import com.bootcamp.identity_service.event.NonCriticalAuditEvent;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserStatusChangedEvent(
        AuditEvent event
) implements CriticalAuditEvent {
    public static UserStatusChangedEvent build(String desc, UUID id) {
        return new UserStatusChangedEvent(
                new AuditEvent(EntityTypeEnum.USER, id,ActionEnum.STATUS_UPDATED,  OffsetDateTime.now(),desc));
    }
    @Override
    public AuditEvent getAuditEvent() {
        return event;
    }

}
