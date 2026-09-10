package com.bootcamp.identity_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.validator.constraints.Length;

@Table(name="roles")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Role extends BaseEntity {

    @Length(max = 50)
    private String name;
    @Length(max = 255)
    private String description;
}
