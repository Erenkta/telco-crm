package com.bootcamp.identity_service.repository;

import com.bootcamp.identity_service.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    boolean existsByName(String name);

    @Query("select r.id from Role r where r.id in :ids")
    Set<UUID> findExistingIds(@Param("ids") Collection<UUID> ids);
}