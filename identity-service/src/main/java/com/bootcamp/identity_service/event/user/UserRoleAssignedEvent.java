package com.bootcamp.identity_service.event.user;

import com.bootcamp.identity_service.entity.enums.ActionEnum;
import com.bootcamp.identity_service.entity.enums.EntityTypeEnum;
import com.bootcamp.identity_service.event.AuditEvent;
import com.bootcamp.identity_service.event.CriticalAuditEvent;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record UserRoleAssignedEvent(
        AuditEvent event
) implements CriticalAuditEvent {
    public static UserRoleAssignedEvent build(List<UUID> roleIds, UUID userId) {
        String defaultMessage = String.format("Requested [%s] roles  are assigned to user %s",roleIds.size(),userId);
        return new UserRoleAssignedEvent(
                new AuditEvent(EntityTypeEnum.USER, userId, ActionEnum.ROLE_ASSIGN,  OffsetDateTime.now(),defaultMessage));
    }
    @Override
    public AuditEvent getAuditEvent() {
        return event;
    }
}
