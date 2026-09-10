package com.bootcamp.identity_service.controller;


import com.bootcamp.identity_service.dto.request.CreatePermissionRequest;
import com.bootcamp.identity_service.dto.response.PermissionResponse;
import com.bootcamp.identity_service.entity.Permission;
import com.bootcamp.identity_service.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping
    public ResponseEntity<PermissionResponse> create(@Valid @RequestBody CreatePermissionRequest permission) {
        PermissionResponse created = permissionService.create(permission);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PermissionResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(permissionService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<PermissionResponse>> getAll() {
        return ResponseEntity.ok(permissionService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<PermissionResponse> update(@PathVariable UUID id,
                                             @Valid @RequestBody CreatePermissionRequest permission) {
        return ResponseEntity.ok(permissionService.update(id, permission));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        permissionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}