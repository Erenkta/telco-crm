package com.bootcamp.identity_service.mapper;

import com.bootcamp.identity_service.dto.request.CreateUserRequest;
import com.bootcamp.identity_service.dto.response.UserResponse;

import com.bootcamp.identity_service.entity.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {
    User toEntity(CreateUserRequest request);
    UserResponse toResponse(User user);

    void updateEntityFromRequest(CreateUserRequest request,@MappingTarget User role );
    @Mapping(target = "roles", source = "roles")
    UserResponse toResponse(User user, List<String> roles);

}
