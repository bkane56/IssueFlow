package com.issueflow.controller;

import com.issueflow.config.TimeConfig;
import com.issueflow.constants.ErrorConstants;
import com.issueflow.dto.request.CreateUserRequest;
import com.issueflow.dto.response.UserResponse;
import com.issueflow.exception.DuplicateResourceException;
import com.issueflow.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
