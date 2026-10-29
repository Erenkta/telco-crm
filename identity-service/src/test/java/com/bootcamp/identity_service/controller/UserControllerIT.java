package com.bootcamp.identity_service.controller;

import com.bootcamp.identity_service.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Full flow: HTTP request -> controller -> service -> real Postgres -> ProblemDetail response.
// No @Transactional here: data is really committed, like in production.
// Each test uses unique usernames/emails, so tests don't collide on unique constraints.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class UserControllerIT {

    private static final String USERS_URL = "/api/v1/users";
    private static final String ROLES_URL = "/api/v1/roles";

    //This will mock the Spring MVC
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createUser_returns409ProblemDetail_whenEmailIsAlreadyUsed() throws Exception {
        String email = unique("dup") + "@mail.com";

        mockMvc.perform(post(USERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserJson(unique("first"), email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(USERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserJson(unique("second"), email)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("CONFLICT error occurred in IDENTITY_SERVICE"))
                .andExpect(jsonPath("$.detail").value(containsString(email)))
                .andExpect(jsonPath("$.instance").value(USERS_URL))
                .andExpect(jsonPath("$.trace").value(""));
    }

    @Test
    void getUser_returns404NotFound_whenUserIsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get(USERS_URL + "/{id}", userId))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value(containsString(userId.toString())))
            .andExpect(jsonPath("$.instance").value(USERS_URL+"/"+userId));
    }

    @Test
    void createUser_returns400ValidationError_whenEmailFormatIsInvalid() throws Exception {
        String email = unique("dup") + "mail.com"; // missing '@' sign

        mockMvc.perform(post(USERS_URL).contentType(MediaType.APPLICATION_JSON).content(
                createUserJson(unique("first"), email)
        ))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors.email[0]").value("Email must be formatted properly"))
                .andExpect(jsonPath("$.instance").value(USERS_URL));
    }

    @Test
    void deleteAssignment_returnsEmptyRole_whenUserRolesAreDeleted() throws Exception {
        String email = unique("dup") + "@mail.com";

        String userResp =  mockMvc.perform(post(USERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserJson(unique("first"), email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String userId = JsonPath.read(userResp,"$.id");

       String roleResp = mockMvc.perform(post(ROLES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRoleJson(unique("ROLE_TESTING"))))
               .andExpect(status().isCreated())
               .andReturn().getResponse().getContentAsString();

        String roleId = JsonPath.read(roleResp,"$.id");

        mockMvc.perform(post(USERS_URL+"/{id}/roles", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createAssignRoleJson(List.of(roleId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isNotEmpty());


        mockMvc.perform(delete(USERS_URL+"/{id}/roles/{roleId}", userId,roleId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(USERS_URL+"/{id}",userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isEmpty());

    }


    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String createUserJson(String username, String email) {
        return """
                {
                  "username": "%s",
                  "email": "%s",
                  "fullName": "Integration Test",
                  "phoneNumber": "+905551112233"
                }
                """.formatted(username, email);
    }

    private static String createRoleJson(String name) {
        return """
                {
                  "name": "%s",
                  "description": "%s"
                }
                """.formatted(name,"description for "+name);
    }
    private static String createAssignRoleJson(List<String> roles) {
        String roleArray = roles.stream()
                .map(role -> "\"" + role + "\"")
                .collect(Collectors.joining(", ", "[", "]"));
        return """
            {
              "roles": %s
            }
            """.formatted(roleArray);
    }
}
