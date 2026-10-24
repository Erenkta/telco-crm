package com.bootcamp.identity_service.event;

public interface NonCriticalAuditEvent {

    AuditEvent getAuditEvent();
}
