package com.bootcamp.identity_service.service.impl;

import com.bootcamp.identity_service.config.CurrentActorProvider;
import com.bootcamp.identity_service.dto.request.CreateUserRequest;
import com.bootcamp.identity_service.dto.response.UserResponse;
import com.bootcamp.identity_service.entity.User;
import com.bootcamp.identity_service.entity.enums.StatusEnum;
import com.bootcamp.identity_service.event.user.UserCreatedEvent;
import com.bootcamp.identity_service.event.user.UserRoleAssignedEvent;
import com.bootcamp.identity_service.event.user.UserRoleRemovedEvent;
import com.bootcamp.identity_service.exception.RecordAlreadyExistException;
import com.bootcamp.identity_service.exception.RecordNotAvailableException;
import com.bootcamp.identity_service.exception.RecordNotFoundException;
import com.bootcamp.identity_service.mapper.UserMapper;
import com.bootcamp.identity_service.repository.RoleRepository;
import com.bootcamp.identity_service.repository.UserRepository;
import com.bootcamp.identity_service.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Quick Learn :
// ExtendWithMocks connect Mockito and JUnit
// @Mock creates a mock object
// @InjectMocks create a real object with mocked dependencies.
// Also MockitoExtension runs in "strict stubs" mode which means unused when throws Unnecessar

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserMapper mapper;
    @Mock private CurrentActorProvider actorProvider;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void create_publishesEvent_whenUsernameAndEmailAreAvailable(){
        UUID userId = UUID.randomUUID();
        User user = User.builder().status(StatusEnum.ACTIVE).build();
        user.setId(userId);
        CreateUserRequest request = new CreateUserRequest(
                UUID.randomUUID(),
                "testuser1",
                "testuser1@mail.com",
                "testuser1",
                "+905555");

        UserResponse response = UserResponse.builder().id(userId).build();
        when(mapper.toResponse(user)).thenReturn(response);
        when(mapper.toEntity(request)).thenReturn(user);
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        UserResponse result = userService.create(request);
        assertThat(result).isEqualTo(response);
        verify(eventPublisher).publishEvent(any(UserCreatedEvent.class));
    }

    @Test
    void createUser_throwsRecordAlreadyExistsException_whenUsernameIsAlreadyRegistered() {
        CreateUserRequest request = new CreateUserRequest(
                UUID.randomUUID(),
                "testuser1",
                "testuser1@mail.com",
                "testuser1",
                "+905555");
        when(userRepository.existsByUsername(request.username())).thenReturn(true);

        assertThatThrownBy(()->userService.create(request))
                .isInstanceOf(RecordAlreadyExistException.class)
                .hasMessageContaining(request.username());

        verify(userRepository,never()).existsByEmail(any());
        verify(userRepository, never()).saveAndFlush(any());
        verify(eventPublisher,never()).publishEvent(any(Object.class));
    }

    @Test
    void createUser_throwsRecordAlreadyExistsException_whenEmailIsAlreadyRegistered() {
        CreateUserRequest request = new CreateUserRequest(
                UUID.randomUUID(),
                "testuser1",
                "testuser1@mail.com",
                "testuser1",
                "+905555");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(()->userService.create(request))
                .isInstanceOf(RecordAlreadyExistException.class)
                .hasMessageContaining(request.email());

        verify(userRepository, never()).saveAndFlush(any());
        verify(eventPublisher,never()).publishEvent(any(Object.class));
    }
    
    @Test
    void assignRoles_throwsNotAvailable_whenUserIsBlocked() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User blockedUser = User.builder().status(StatusEnum.BLOCKED).build();
        when(userRepository.findActiveByIdForUpdate(userId)).thenReturn(Optional.of(blockedUser));

        // Act + Assert
        assertThatThrownBy(() -> userService.assignRoles(List.of(roleId), userId))
                .isInstanceOf(RecordNotAvailableException.class)
                .hasMessageContaining("BLOCKED");

        // Assert: the rule must stop the flow before anything is written
        verify(userRoleRepository, never()).assignIfAbsent(any(), anyCollection(), anyString());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void assignRoles_throwsNotFound_whenSomeRolesDoNotExist(){
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID nonExistingRoleId = UUID.randomUUID();

        User user = User.builder().status(StatusEnum.ACTIVE).build();
        when(userRepository.findActiveByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findExistingIds(Set.of(roleId,nonExistingRoleId))).thenReturn(Set.of(roleId));

        assertThatThrownBy(() -> userService.assignRoles(List.of(roleId,nonExistingRoleId), userId))
        .isInstanceOf(RecordNotFoundException.class)
                .hasMessageContaining(nonExistingRoleId.toString());

        verify(userRoleRepository, never()).assignIfAbsent(any(), anyCollection(), anyString());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
        verify(mapper,never()).toResponse(any(),anyList());
    }

    @Test
    void assignRoles_publishesEvent_whenAtLeastOneRoleIsNewlyAssigned() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User activeUser = User.builder().status(StatusEnum.ACTIVE).build();
        when(userRepository.findActiveByIdForUpdate(userId)).thenReturn(Optional.of(activeUser));
        when(roleRepository.findExistingIds(Set.of(roleId))).thenReturn(Set.of(roleId));
        when(actorProvider.currentActor()).thenReturn("system");
        when(userRoleRepository.assignIfAbsent(userId, Set.of(roleId), "system")).thenReturn(1);

        userService.assignRoles(List.of(roleId), userId);

        verify(eventPublisher).publishEvent(any(UserRoleAssignedEvent.class));
    }

    @Test
    void assignRoles_doesNotPublishEvent_whenAllRolesWereAlreadyAssigned() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User activeUser = User.builder().status(StatusEnum.ACTIVE).build();
        when(userRepository.findActiveByIdForUpdate(userId)).thenReturn(Optional.of(activeUser));
        when(roleRepository.findExistingIds(Set.of(roleId))).thenReturn(Set.of(roleId));
        when(actorProvider.currentActor()).thenReturn("system");
        when(userRoleRepository.assignIfAbsent(userId, Set.of(roleId), "system")).thenReturn(0);

        userService.assignRoles(List.of(roleId), userId);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void removeRoles_throwsRecordNotFound_whenUserIsNotFound(){
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        when(userRepository.findByIdAndStatusNot(userId,StatusEnum.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(()->userService.removeRoles(roleId,userId))
                .isInstanceOf(RecordNotFoundException.class)
                .hasMessageContaining(userId.toString());
        verify(eventPublisher,never()).publishEvent(any(Object.class));
        verify(userRoleRepository,never()).deleteAssignment(any(),any());
    }

    @Test
    void removeRoles_throwsRecordNotAvailableException_whenUserStatusIsBlocked(){
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        User user = User.builder().status(StatusEnum.BLOCKED).build();
        when(userRepository.findByIdAndStatusNot(userId,StatusEnum.DELETED)).thenReturn(Optional.of(user));

        assertThatThrownBy(()->userService.removeRoles(roleId,userId))
                .isInstanceOf(RecordNotAvailableException.class)
                .hasMessageContaining("BLOCKED");

        verify(eventPublisher,never()).publishEvent(any(Object.class));
        verify(userRoleRepository,never()).deleteAssignment(any(),any());
    }

    @Test
    void removeRoles_publishesEvent_whenAtLeastOneRoleIsRemoved() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User user = User.builder().status(StatusEnum.ACTIVE).build();

        when(userRepository.findByIdAndStatusNot(userId,StatusEnum.DELETED)).thenReturn(Optional.of(user));
        when(userRoleRepository.deleteAssignment(userId,roleId)).thenReturn(1);

        userService.removeRoles(roleId, userId);

        verify(eventPublisher).publishEvent(any(UserRoleRemovedEvent.class));
    }

    @Test
    void removeRoles_doesNotPublishEvent_whenNoRoleIsRemoved() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User user = User.builder().status(StatusEnum.ACTIVE).build();

        when(userRepository.findByIdAndStatusNot(userId,StatusEnum.DELETED)).thenReturn(Optional.of(user));
        when(userRoleRepository.deleteAssignment(userId,roleId)).thenReturn(0);

        userService.removeRoles(roleId, userId);

        verify(eventPublisher,never()).publishEvent(any(UserRoleRemovedEvent.class));
    }
}
