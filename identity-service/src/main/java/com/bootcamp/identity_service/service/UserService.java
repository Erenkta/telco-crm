package com.bootcamp.identity_service.service;

import com.bootcamp.identity_service.dto.request.CreateUserRequest;
import com.bootcamp.identity_service.dto.response.UserResponse;
import com.bootcamp.identity_service.entity.User;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponse create(CreateUserRequest user);
    UserResponse getById(UUID id);
    List<UserResponse> getAll();
    UserResponse update(UUID id, CreateUserRequest user);
    void delete(UUID id);
}