package com.ogreten.catan.auth.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ogreten.catan.auth.domain.CustomUserDetails;
import com.ogreten.catan.auth.domain.User;
import com.ogreten.catan.auth.repository.UserRepository;
import com.ogreten.catan.auth.schema.UserCreateIn;
import com.ogreten.catan.auth.schema.UserWithoutPasswordOut;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "User", description = "User Management API")
@RequestMapping("api/user")
@RestController
public class UserController {

    private final UserRepository userRepository;

    public UserController(
            UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Operation(summary = "Register a new user", description = "Register a new user", tags = { "user", "post" })
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {
                    @Content(schema = @Schema(implementation = UserWithoutPasswordOut.class), mediaType = "application/json") }),
    })
    @PostMapping("/register")
    public UserWithoutPasswordOut register(
            @Parameter(description = "JSON for user to be created. It contains email, password, firstName and lastName.") @RequestBody @Valid UserCreateIn userIn) {

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(userIn.getPassword());

        User user = new User();
        user.setEmail(userIn.getEmail());
        user.setPassword(encodedPassword);
        user.setFirstName(userIn.getFirstName());
        user.setLastName(userIn.getLastName());
        user = userRepository.save(user);

        return UserWithoutPasswordOut.fromUser(user);
    }

    @Operation(summary = "Get logged in user", description = "Get logged in user", tags = { "user", "get" })
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {
                    @Content(schema = @Schema(implementation = UserWithoutPasswordOut.class), mediaType = "application/json") }),
    })
    @GetMapping("/login")
    public UserWithoutPasswordOut login(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        User user = customUserDetails.getUser();
        return UserWithoutPasswordOut.fromUser(user);
    }
}
