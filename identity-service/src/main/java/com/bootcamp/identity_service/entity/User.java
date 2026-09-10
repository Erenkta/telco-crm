package com.bootcamp.identity_service.entity;

import com.bootcamp.identity_service.entity.enums.StatusEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.Version;

import java.util.UUID;

@Table(name = "users")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class User extends BaseEntity {

    private UUID customerId;

    @Length(max = 100)
    private String username;

    @Email
    private String email;

    @Length(max = 150)
    private String fullName;

    @Length(max = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private StatusEnum status;

    @Column(name = "keycloak_user_id",nullable = false,unique = true)
    private String keycloakUserId;

    @Version
    private Long version;

}
