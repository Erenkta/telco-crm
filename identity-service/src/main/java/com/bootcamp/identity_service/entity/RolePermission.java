package com.bootcamp.identity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table(name = "role_permissions",uniqueConstraints = @UniqueConstraint(
        name = "uk_role_permission",
        columnNames = {"role_id","permission_id"}
))
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class RolePermission {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Permission permission;

    private OffsetDateTime assigned_at;
    private String assignedBy;
}
