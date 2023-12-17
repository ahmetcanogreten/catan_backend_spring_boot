package com.ogreten.catan.auth.controller;

import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ogreten.catan.auth.config.SpringSecurityConfig;
import com.ogreten.catan.auth.domain.User;
import com.ogreten.catan.auth.repository.UserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;

// @WebMvcTest(UserController.class)
// @Import(SpringSecurityConfig.class)
@SpringBootTest
class UserControllerTest {

    @Autowired
    private WebApplicationContext context;

    MockMvc mockMvc;

    // @MockBean
    // UserRepository userRepository;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser()
    void testLogin() throws Exception {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        UUID id = UUID.randomUUID();
        String email = "test@test.com";
        String password = "password";
        String encodedPassword = passwordEncoder.encode(password);
        String firstName = "Test";
        String lastName = "User";

        User user = new User(
                id,
                email,
                encodedPassword,
                firstName,
                lastName);

        // when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/user/login").with(httpBasic(email, password)))
                .andExpect(MockMvcResultMatchers.status().isOk());

    }

    @Test
    void testRegister() {

    }
}
