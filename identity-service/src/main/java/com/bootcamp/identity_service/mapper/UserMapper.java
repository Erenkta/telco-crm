package com.bootcamp.identity_service.mapper;

import com.bootcamp.identity_service.dto.request.CreateUserRequest;
import com.bootcamp.identity_service.dto.response.UserResponse;

import com.bootcamp.identity_service.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {
    User toEntity(CreateUserRequest request);
    User toEntity(UserResponse response);
    UserResponse toResponse(User user);

    void updateEntityFromRequest(CreateUserRequest request,@MappingTarget User role );
}
