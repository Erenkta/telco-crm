package com.bootcamp.identity_service.exception;

import com.bootcamp.common.exception.UniqueConstraintMessages;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class IdentityUniqueConstraintMessages implements UniqueConstraintMessages {
    @Override
    public Map<String, String> messages() {
        return Map.of(
                "users_email_key", "Email is already in use",
                "users_username_key", "Username is already in use",
                "roles_name_key", "Role name is already in use",
                "permissions_name_key", "Permission name is already in use",
                "uk_user_role", "Role is already assigned to the user"
        );
    }
}
