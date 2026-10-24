package com.bootcamp.identity_service.service;

import com.bootcamp.identity_service.dto.request.CreatePermissionRequest;
import com.bootcamp.identity_service.dto.response.PageResponse;
import com.bootcamp.identity_service.dto.response.PermissionResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PermissionService {
    PermissionResponse create(CreatePermissionRequest permission);
    PermissionResponse getById(UUID id);
    PageResponse<PermissionResponse> getAll(Pageable pageable);
    PermissionResponse update(UUID id, CreatePermissionRequest permission);
    void delete(UUID id);
}