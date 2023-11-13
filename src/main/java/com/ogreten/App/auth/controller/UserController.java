package com.ogreten.App.auth.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ogreten.App.auth.domain.CustomUserDetails;
import com.ogreten.App.auth.domain.User;
import com.ogreten.App.auth.repository.UserRepository;

@RequestMapping("api/user")
@RestController
public class UserController {

    private final UserRepository userRepository;

    public UserController(
            UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public User register(@RequestBody User user) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        return userRepository.save(user);
    }

    @GetMapping("/login")
    public User login(@AuthenticationPrincipal CustomUserDetails user) {
        return user.getUser();
    }
}
