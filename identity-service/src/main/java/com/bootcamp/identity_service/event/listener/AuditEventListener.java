package com.bootcamp.identity_service.event.listener;

import com.bootcamp.identity_service.config.CurrentActorProvider;
import com.bootcamp.identity_service.entity.IdentityAuditLog;
import com.bootcamp.identity_service.event.AuditEvent;
import com.bootcamp.identity_service.event.CriticalAuditEvent;
import com.bootcamp.identity_service.event.NonCriticalAuditEvent;
import com.bootcamp.identity_service.repository.IdentityAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Component
@Slf4j
@RequiredArgsConstructor
public class AuditEventListener {
    private final IdentityAuditLogRepository repository;
    private final CurrentActorProvider actorProvider;


    @EventListener
    public void onCriticalEvent(CriticalAuditEvent event){
            log.debug ("A critical audit event received: {}", event);
            AuditEvent publishedEvent = event.getAuditEvent();
            IdentityAuditLog audit = toAuditLog(publishedEvent);
            repository.save(audit);
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onNonCriticalEvent(NonCriticalAuditEvent event){
        try {
            log.debug("A non critical audit event received: {}", event);
            AuditEvent publishedEvent = event.getAuditEvent();
            IdentityAuditLog audit = toAuditLog(publishedEvent);
            repository.save(audit);
        }catch (Exception ex){
             log.error("Non-critical audit write failed: {}", event, ex);
        }

    }
    private IdentityAuditLog toAuditLog(AuditEvent publishedEvent){
        return  new IdentityAuditLog().toBuilder()
                .action(publishedEvent.actionType())
                .createdAt(publishedEvent.auditedAt())
                .detail(publishedEvent.desc())
                .entityType(publishedEvent.object())
                .entityId(publishedEvent.objectId())
                .performedBy(actorProvider.currentActor())
                .build();
    }
}
