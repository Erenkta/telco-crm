package com.bootcamp.identity_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.validator.constraints.Length;

@Table(name = "permissions")
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class Permission extends BaseEntity
{
    @Length(max = 100)
    private String name;
    @Length(max = 255)
    private String description;
}
