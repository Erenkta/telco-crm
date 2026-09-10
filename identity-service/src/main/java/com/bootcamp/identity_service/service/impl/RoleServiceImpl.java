package com.bootcamp.identity_service.service.impl;

import com.bootcamp.identity_service.dto.request.CreateRoleRequest;
import com.bootcamp.identity_service.dto.response.RoleResponse;
import com.bootcamp.identity_service.entity.Role;
import com.bootcamp.identity_service.mapper.RoleMapper;
import com.bootcamp.identity_service.mapper.UserMapper;
import com.bootcamp.identity_service.repository.RoleRepository;
import com.bootcamp.identity_service.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper mapper;

    @Override
    @Transactional
    public RoleResponse create(CreateRoleRequest role) {
        return mapper.toResponse(roleRepository.save(mapper.toEntity(role)));
    }

    @Override
    public RoleResponse getById(UUID id) {
        return roleRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Role not found with id: " + id));
    }

    @Override
    public List<RoleResponse> getAll() {
        return roleRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public RoleResponse update(UUID id, CreateRoleRequest role) {
        Role existingRole = mapper.toEntity(getById(id));
        mapper.updateEntityFromRequest(role,existingRole);
        return mapper.toResponse(roleRepository.save(existingRole));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!roleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Role not found with id: " + id);
        }
        roleRepository.deleteById(id);
    }
}