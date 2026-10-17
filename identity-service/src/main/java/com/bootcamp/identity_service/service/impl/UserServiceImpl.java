package com.bootcamp.identity_service.service.impl;

import com.bootcamp.identity_service.dto.request.CreateUserRequest;
import com.bootcamp.identity_service.dto.response.UserResponse;
import com.bootcamp.identity_service.entity.User;
import com.bootcamp.identity_service.exception.RecordAlreadyExistException;
import com.bootcamp.identity_service.exception.RecordNotFoundException;
import com.bootcamp.identity_service.mapper.UserMapper;
import com.bootcamp.identity_service.repository.UserRepository;
import com.bootcamp.identity_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public UserResponse getById(UUID id) {
        return userRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new RecordNotFoundException("User not found with id: " + id));
    }

    @Override
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public UserResponse update(UUID id, CreateUserRequest user) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RecordNotFoundException("User not found with id: " + id));
        mapper.updateEntityFromRequest(user, existingUser);
        return mapper.toResponse(userRepository.saveAndFlush(existingUser));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new RecordNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}