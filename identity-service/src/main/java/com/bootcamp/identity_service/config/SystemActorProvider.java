package com.bootcamp.identity_service.config;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class SystemActorProvider implements CurrentActorProvider {
    @Override
    public String currentActor() { return "CRM-SYSTEM"; }
}
