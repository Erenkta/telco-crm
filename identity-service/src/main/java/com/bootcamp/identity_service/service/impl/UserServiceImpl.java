package com.bootcamp.identity_service.service.impl;

import com.bootcamp.identity_service.dto.request.CreateUserRequest;
import com.bootcamp.identity_service.dto.response.UserResponse;
import com.bootcamp.identity_service.entity.User;
import com.bootcamp.identity_service.entity.enums.StatusEnum;
import com.bootcamp.identity_service.exception.RecordAlreadyExistException;
import com.bootcamp.identity_service.exception.RecordNotAvailableException;
import com.bootcamp.identity_service.exception.RecordNotFoundException;
import com.bootcamp.identity_service.mapper.UserMapper;
import com.bootcamp.identity_service.repository.RoleRepository;
import com.bootcamp.identity_service.repository.UserRepository;
import com.bootcamp.identity_service.repository.UserRoleRepository;
import com.bootcamp.identity_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final UserMapper mapper;
    private static final String SYSTEM_USER = "system";

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest user) {
        if(userRepository.existsByUsername(user.username())){
            String message = String.format("User with username %s already exists",user.username());
            throw new RecordAlreadyExistException(message);
        }
        if(userRepository.existsByEmail(user.email())){
            String message = String.format("User with email %s already exists",user.email());
            throw new RecordAlreadyExistException(message);
        }
        return mapper.toResponse(userRepository.saveAndFlush(mapper.toEntity(user)));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        User user = findByIdOrThrowEx(id);
        return mapper.toResponse(user, userRoleRepository.findRoleNamesByUserId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAllByStatusNot(StatusEnum.DELETED).stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public UserResponse update(UUID id, CreateUserRequest user) {
        User existingUser = findByIdOrThrowEx(id);
        throwIfNotModifiable(existingUser, "update");
        mapper.updateEntityFromRequest(user, existingUser);
        return mapper.toResponse(userRepository.saveAndFlush(existingUser));
    }

    @Override
    @Transactional
    public UserResponse updateStatus(UUID id, StatusEnum status) {
        User user = findByIdOrThrowEx(id);
        user.changeStatus(status);
        return mapper.toResponse(userRepository.saveAndFlush(user),userRoleRepository.findRoleNamesByUserId(id));
    }


    @Override
    @Transactional
    public UserResponse assignRoles(List<UUID> roleIds, UUID userId) {
        User existingUser = findByIdOrThrowExWithLock(userId);
        throwIfNotModifiable(existingUser, "assign roles to");
        Set<UUID> requested = new HashSet<>(roleIds);

        // Fetch the existing role ids. Some of the requested ids might not exist.
        Set<UUID> existing = roleRepository.findExistingIds(requested);
        if (existing.size() != requested.size()) {
            requested.removeAll(existing);
            throw new RecordNotFoundException("Roles not found with ids: " + requested);
        }

        userRoleRepository.assignIfAbsent(userId, existing, SYSTEM_USER);
        return mapper.toResponse(existingUser, userRoleRepository.findRoleNamesByUserId(userId));
    }

    @Override
    @Transactional
    public void removeRoles(UUID roleId, UUID userId) {
        User user = findByIdOrThrowEx(userId);
        throwIfNotModifiable(user, "remove roles from");
        userRoleRepository.deleteAssignment(userId, roleId);
    }

    private void throwIfNotModifiable(User user, String action) {
        if (!user.isModifiable()) {
            String message = String.format("Cannot %s user with id %s because the user status is %s",
                    action, user.getId(), user.getStatus());
            throw new RecordNotAvailableException(message);
        }
    }

    private User findByIdOrThrowEx(UUID id){
        return userRepository.findByIdAndStatusNot(id,StatusEnum.DELETED)
                .orElseThrow(() -> new RecordNotFoundException("User not found with id: " + id));
    }
    private User findByIdOrThrowExWithLock(UUID id){
        return userRepository.findActiveByIdForUpdate(id).orElseThrow(() -> new RecordNotFoundException("User not found with id: " + id));
    }
}