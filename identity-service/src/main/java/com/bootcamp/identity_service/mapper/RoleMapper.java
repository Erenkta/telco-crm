package com.bootcamp.identity_service.mapper;

import com.bootcamp.identity_service.dto.request.CreateRoleRequest;
import com.bootcamp.identity_service.dto.response.RoleResponse;
import com.bootcamp.identity_service.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RoleMapper {

    Role toEntity(CreateRoleRequest request);
    Role toEntity(RoleResponse response);
    RoleResponse toResponse(Role role);

    void updateEntityFromRequest(CreateRoleRequest request,@MappingTarget Role role );
}
