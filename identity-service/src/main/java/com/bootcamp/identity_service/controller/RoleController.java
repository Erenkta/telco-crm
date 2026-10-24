package com.bootcamp.identity_service.controller;

import com.bootcamp.identity_service.dto.request.CreateRoleRequest;
import com.bootcamp.identity_service.dto.response.PageResponse;
import com.bootcamp.identity_service.dto.response.RoleResponse;
import com.bootcamp.identity_service.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody CreateRoleRequest role) {
        RoleResponse created = roleService.create(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(roleService.getById(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<RoleResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(roleService.getAll(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> update(@PathVariable UUID id,
                                       @Valid @RequestBody CreateRoleRequest role) {
        return ResponseEntity.ok(roleService.update(id, role));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}