package com.bootcamp.identity_service.repository;

import com.bootcamp.identity_service.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    @Modifying
    @Query(value = """
        INSERT INTO user_roles (user_id, role_id, assigned_by)
        SELECT :userId, r.id, :assignedBy FROM roles r WHERE r.id IN (:roleIds)
        ON CONFLICT (user_id, role_id) DO NOTHING
        """, nativeQuery = true)
    int assignIfAbsent(@Param("userId") UUID userId,
                       @Param("roleIds") Collection<UUID> roleIds,
                       @Param("assignedBy") String assignedBy);

    @Query("select r.name from UserRole ur join ur.role r where ur.user.id = :userId order by r.name")
    List<String> findRoleNamesByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("delete from UserRole ur where ur.user.id = :userId and ur.role.id = :roleId")
    int deleteAssignment(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

}
