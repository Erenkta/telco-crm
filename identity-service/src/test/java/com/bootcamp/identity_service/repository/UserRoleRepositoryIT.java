package com.bootcamp.identity_service.repository;

import com.bootcamp.identity_service.TestcontainersConfiguration;
import com.bootcamp.identity_service.entity.Role;
import com.bootcamp.identity_service.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Roles come from the Flyway seed migration, so no role setup is needed here.
// @DataJpaTest wraps each test in a transaction that is rolled back afterwards.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class UserRoleRepositoryIT {

    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    private UUID userId;
    private UUID customerRoleId;
    private UUID billingRoleId;

    @BeforeEach
    void setUp() {
        User user = userRepository.saveAndFlush(User.builder()
                .username("repo-test-user")
                .email("repo-test@mail.com")
                .fullName("Repo Test")
                .keycloakUserId(UUID.randomUUID().toString())
                .build());
        userId = user.getId();
        customerRoleId = roleIdByName("ROLE_CUSTOMER");
        billingRoleId = roleIdByName("ROLE_BILLING_OPERATOR");
    }

    @Test
    void assignIfAbsent_insertsAllRoles_whenNoneAreAssigned() {
        int inserted = userRoleRepository.assignIfAbsent(userId, Set.of(customerRoleId, billingRoleId), "system");

        assertThat(inserted).isEqualTo(2);
        assertThat(userRoleRepository.findRoleNamesByUserId(userId))
                .containsExactly("ROLE_BILLING_OPERATOR", "ROLE_CUSTOMER");
    }

    @Test
    void assignIfAbsent_returnsZero_andCreatesNoDuplicates_whenRolesAreAlreadyAssigned() {
        userRoleRepository.assignIfAbsent(userId, Set.of(customerRoleId), "system");

        int insertedAgain = userRoleRepository.assignIfAbsent(userId, Set.of(customerRoleId), "system");

        assertThat(insertedAgain).isZero();
        assertThat(userRoleRepository.findRoleNamesByUserId(userId)).containsExactly("ROLE_CUSTOMER");
    }

    @Test
    void assignIfAbsent_insertsOnlyNewRoles_whenSomeAreAlreadyAssigned() {
        userRoleRepository.assignIfAbsent(userId, Set.of(customerRoleId), "system");

        int inserted = userRoleRepository.assignIfAbsent(userId, Set.of(customerRoleId, billingRoleId), "system");

        assertThat(inserted).isEqualTo(1);
        assertThat(userRoleRepository.findRoleNamesByUserId(userId))
                .containsExactly("ROLE_BILLING_OPERATOR", "ROLE_CUSTOMER");
    }

    @Test
    void deleteAssignment_removesRole_whenRoleIsAssigned(){
       //First assign the role so "when" condition is okay
       userRoleRepository.assignIfAbsent(userId, Set.of(customerRoleId), "system");

       int deletedCount = userRoleRepository.deleteAssignment(userId,customerRoleId);

       assertThat(deletedCount).isEqualTo(1);
       assertThat(userRoleRepository.findRoleNamesByUserId(userId)).isEmpty();
    }

    @Test
    void deleteAssignment_returnsZero_WhenRoleIsNotAssigned(){
        userRoleRepository.assignIfAbsent(userId, Set.of(customerRoleId), "system");
        int deletedCount = userRoleRepository.deleteAssignment(userId,billingRoleId);

        assertThat(deletedCount).isZero();
        assertThat(userRoleRepository.findRoleNamesByUserId(userId)).containsExactly("ROLE_CUSTOMER");
    }

    @Test
    void deleteAssignment_removesOnlyGivenRole_whenUserHasMultipleRoles(){
        userRoleRepository.assignIfAbsent(userId, Set.of(customerRoleId,billingRoleId), "system");
        int deletedCount = userRoleRepository.deleteAssignment(userId,billingRoleId);

        assertThat(deletedCount).isEqualTo(1);
        assertThat(userRoleRepository.findRoleNamesByUserId(userId)).containsExactly("ROLE_CUSTOMER");
    }
    private UUID roleIdByName(String name) {
        List<Role> roles = roleRepository.findAll();
        return roles.stream()
                .filter(role -> role.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seed role missing: " + name))
                .getId();
    }
}
