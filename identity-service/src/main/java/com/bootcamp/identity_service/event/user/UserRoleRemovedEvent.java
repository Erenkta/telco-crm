package com.bootcamp.identity_service.event.user;

import com.bootcamp.identity_service.entity.enums.ActionEnum;
import com.bootcamp.identity_service.entity.enums.EntityTypeEnum;
import com.bootcamp.identity_service.event.AuditEvent;
import com.bootcamp.identity_service.event.CriticalAuditEvent;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserRoleRemovedEvent(
        AuditEvent event

) implements CriticalAuditEvent {
    public static UserRoleRemovedEvent build(UUID roleId, UUID userId){
        String defaultMessage = String.format("Role %s is removed from user %s",roleId,userId);
        return new UserRoleRemovedEvent(
                new AuditEvent(EntityTypeEnum.USER, userId, ActionEnum.ROLE_REMOVE,  OffsetDateTime.now(),defaultMessage));
    }
    @Override
    public AuditEvent getAuditEvent() {
        return event;
    }
}
