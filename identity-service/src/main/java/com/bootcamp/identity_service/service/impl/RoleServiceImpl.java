package com.bootcamp.identity_service.service.impl;

import com.bootcamp.identity_service.dto.request.CreateRoleRequest;
import com.bootcamp.identity_service.dto.response.PageResponse;
import com.bootcamp.identity_service.dto.response.RoleResponse;
import com.bootcamp.identity_service.entity.Role;
import com.bootcamp.identity_service.event.role.RoleCreatedEvent;
import com.bootcamp.identity_service.event.role.RoleDeletedEvent;
import com.bootcamp.identity_service.event.role.RoleModifiedEvent;
import com.bootcamp.identity_service.exception.RecordAlreadyExistException;
import com.bootcamp.identity_service.exception.RecordNotFoundException;
import com.bootcamp.identity_service.exception.enums.ModuleEnum;
import com.bootcamp.identity_service.mapper.RoleMapper;
import com.bootcamp.identity_service.repository.RoleRepository;
import com.bootcamp.identity_service.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public RoleResponse create(CreateRoleRequest role) {
        if(roleRepository.existsByName(role.name())){
            String message = String.format("Role with name %s already exists",role.name());
            throw new RecordAlreadyExistException(message, ModuleEnum.IDENTITY_SERVICE);
        }
        RoleResponse response = mapper.toResponse(roleRepository.saveAndFlush(mapper.toEntity(role)));
        eventPublisher.publishEvent(RoleCreatedEvent.build(response.id()));
        return response;
    }

    @Override
    public RoleResponse getById(UUID id) {
        return roleRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new RecordNotFoundException("Role not found with id: " + id));
    }

    @Override
    public PageResponse<RoleResponse> getAll(Pageable pageable) {
        return new PageResponse<>(roleRepository.findAll(pageable).map(mapper::toResponse));
    }

    @Override
    @Transactional
    public RoleResponse update(UUID id, CreateRoleRequest role) {
        Role existingRole = roleRepository.findById(id)
                .orElseThrow(() -> new RecordNotFoundException("Role not found with id: " + id));
        mapper.updateEntityFromRequest(role,existingRole);
        RoleResponse response = mapper.toResponse(roleRepository.saveAndFlush(existingRole));
        eventPublisher.publishEvent(RoleModifiedEvent.build(id));

        return response;
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!roleRepository.existsById(id)) {
            throw new RecordNotFoundException("Role not found with id: " + id);
        }
        roleRepository.deleteById(id);
        eventPublisher.publishEvent(RoleDeletedEvent.build(id));
    }
}