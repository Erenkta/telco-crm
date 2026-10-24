package com.bootcamp.identity_service.service.impl;

import com.bootcamp.identity_service.dto.request.CreateRoleRequest;
import com.bootcamp.identity_service.dto.response.PageResponse;
import com.bootcamp.identity_service.dto.response.RoleResponse;
import com.bootcamp.identity_service.entity.Role;
import com.bootcamp.identity_service.exception.RecordAlreadyExistException;
import com.bootcamp.identity_service.exception.RecordNotFoundException;
import com.bootcamp.identity_service.exception.enums.ModuleEnum;
import com.bootcamp.identity_service.mapper.RoleMapper;
import com.bootcamp.identity_service.repository.RoleRepository;
import com.bootcamp.identity_service.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper mapper;

    @Override
    @Transactional
    public RoleResponse create(CreateRoleRequest role) {
        if(roleRepository.existsByName(role.name())){
            String message = String.format("Role with name %s already exists",role.name());
            throw new RecordAlreadyExistException(message, ModuleEnum.IDENTITY_SERVICE);
        }
        return mapper.toResponse(roleRepository.saveAndFlush(mapper.toEntity(role)));
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
        return mapper.toResponse(roleRepository.saveAndFlush(existingRole));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!roleRepository.existsById(id)) {
            throw new RecordNotFoundException("Role not found with id: " + id);
        }
        roleRepository.deleteById(id);
    }
}