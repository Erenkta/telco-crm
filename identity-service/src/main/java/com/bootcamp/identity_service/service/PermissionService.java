package com.bootcamp.identity_service.service;

import com.bootcamp.identity_service.dto.request.CreatePermissionRequest;
import com.bootcamp.identity_service.dto.response.PermissionResponse;
import com.bootcamp.identity_service.entity.Permission;

import java.util.List;
import java.util.UUID;

public interface PermissionService {
    PermissionResponse create(CreatePermissionRequest permission);
    PermissionResponse getById(UUID id);
    List<PermissionResponse> getAll();
    PermissionResponse update(UUID id, CreatePermissionRequest permission);
    void delete(UUID id);
}