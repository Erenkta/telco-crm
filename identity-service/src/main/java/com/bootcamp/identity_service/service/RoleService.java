package com.bootcamp.identity_service.service;

import com.bootcamp.identity_service.dto.request.CreateRoleRequest;
import com.bootcamp.identity_service.dto.response.PageResponse;
import com.bootcamp.identity_service.dto.response.RoleResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RoleService {
    RoleResponse create(CreateRoleRequest role);
    RoleResponse getById(UUID id);
    PageResponse<RoleResponse> getAll(Pageable pageable);
    RoleResponse update(UUID id, CreateRoleRequest role);
    void delete(UUID id);
}