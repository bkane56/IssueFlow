package com.issueflow.controller;

import com.issueflow.config.TimeConfig;
import com.issueflow.constants.ErrorConstants;
import com.issueflow.dto.request.CreateUserRequest;
import com.issueflow.dto.response.UserResponse;
import com.issueflow.exception.DuplicateResourceException;
import com.issueflow.exception.ResourceNotFoundException;
import com.issueflow.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(TimeConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void listsUsers() throws Exception {
        when(userService.findAll()).thenReturn(List.of(
                new UserResponse(3L, "Alex Chen", "alex.chen@issueflow.local", true)
        ));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].name").value("Alex Chen"));
    }

    @Test
    void returnsUserById() throws Exception {
        when(userService.findById(3L))
                .thenReturn(new UserResponse(3L, "Alex Chen", "alex.chen@issueflow.local", true));

        mockMvc.perform(get("/api/users/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.email").value("alex.chen@issueflow.local"));
    }

    @Test
    void returnsNotFoundForMissingUser() throws Exception {
        when(userService.findById(1042L))
                .thenThrow(new ResourceNotFoundException(ErrorConstants.USER_NOT_FOUND.formatted(1042L)));

        mockMvc.perform(get("/api/users/1042"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(ErrorConstants.USER_NOT_FOUND.formatted(1042L)))
                .andExpect(jsonPath("$.path").value("/api/users/1042"));
    }

    @Test
    void createsUser() throws Exception {
        when(userService.create(any(CreateUserRequest.class)))
                .thenReturn(new UserResponse(8L, "Casey Nguyen", "casey.nguyen@issueflow.local", true));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Casey Nguyen",
                                  "email": "casey.nguyen@issueflow.local"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.name").value("Casey Nguyen"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void rejectsInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void returnsConflictForDuplicateEmail() throws Exception {
        when(userService.create(any(CreateUserRequest.class)))
                .thenThrow(new DuplicateResourceException(ErrorConstants.EMAIL_IN_USE));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Alex Chen",
                                  "email": "alex.chen@issueflow.local"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(ErrorConstants.EMAIL_IN_USE));
    }
}
