package com.bootcamp.identity_service.service.impl;

import com.bootcamp.identity_service.dto.request.CreateUserRequest;
import com.bootcamp.identity_service.dto.response.UserResponse;
import com.bootcamp.identity_service.entity.User;
import com.bootcamp.identity_service.mapper.UserMapper;
import com.bootcamp.identity_service.repository.UserRepository;
import com.bootcamp.identity_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest user) {
        return mapper.toResponse(userRepository.save(mapper.toEntity(user)));
    }

    @Override
    public UserResponse getById(UUID id) {
        return userRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found with id: " + id));
    }

    @Override
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public UserResponse update(UUID id, CreateUserRequest user) {
        User existingUser = mapper.toEntity(getById(id));
        mapper.updateEntityFromRequest(user, existingUser);
        return mapper.toResponse(userRepository.save(existingUser));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}