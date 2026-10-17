package com.bootcamp.identity_service.service;

import com.bootcamp.identity_service.dto.request.CreateRoleRequest;
import com.bootcamp.identity_service.dto.response.RoleResponse;
import com.bootcamp.identity_service.entity.Role;

import java.util.List;
import java.util.UUID;

public interface RoleService {
    RoleResponse create(CreateRoleRequest role);
    RoleResponse getById(UUID id);
    List<RoleResponse> getAll();
    RoleResponse update(UUID id, CreateRoleRequest role);
    void delete(UUID id);
}