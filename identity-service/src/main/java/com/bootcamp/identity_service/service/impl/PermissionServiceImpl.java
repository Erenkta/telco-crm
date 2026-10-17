package com.bootcamp.identity_service.service.impl;

import com.bootcamp.identity_service.dto.request.CreatePermissionRequest;
import com.bootcamp.identity_service.dto.response.PermissionResponse;
import com.bootcamp.identity_service.entity.Permission;
import com.bootcamp.identity_service.mapper.PermissionMapper;
import com.bootcamp.identity_service.repository.PermissionRepository;
import com.bootcamp.identity_service.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper mapper;

    @Override
    @Transactional
    public PermissionResponse create(CreatePermissionRequest permission) {
        return mapper.toResponse(permissionRepository.save(mapper.toEntity(permission)));
    }

    @Override
    public PermissionResponse getById(UUID id) {
        return permissionRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Permission not found with id: " + id));
    }

    @Override
    public List<PermissionResponse> getAll() {
        return permissionRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public PermissionResponse update(UUID id, CreatePermissionRequest permission) {
        Permission existingPermission = mapper.toEntity(getById(id));
        mapper.updateEntityFromRequest(permission,existingPermission);
        return mapper.toResponse(permissionRepository.save(existingPermission));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!permissionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Permission not found with id: " + id);
        }
        permissionRepository.deleteById(id);
    }
}