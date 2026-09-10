package com.bootcamp.identity_service.mapper;

import com.bootcamp.identity_service.dto.request.CreatePermissionRequest;
import com.bootcamp.identity_service.dto.response.PermissionResponse;
import com.bootcamp.identity_service.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PermissionMapper {

    PermissionResponse toResponse(Permission permission); // Read
    Permission toEntity(CreatePermissionRequest request); // Create
    Permission toEntity(PermissionResponse request); // Create

    void updateEntityFromRequest(CreatePermissionRequest request, @MappingTarget Permission permission); // Update
}
