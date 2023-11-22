package com.ogreten.catan.auth.schema;

import java.util.UUID;

import com.ogreten.catan.auth.domain.User;

import lombok.Data;

@Data
public class UserWithoutPassword {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;

    public static UserWithoutPassword fromUser(User user) {
        UserWithoutPassword userWithoutPassword = new UserWithoutPassword();
        userWithoutPassword.setId(user.getId());
        userWithoutPassword.setEmail(user.getEmail());
        userWithoutPassword.setFirstName(user.getFirstName());
        userWithoutPassword.setLastName(user.getLastName());
        return userWithoutPassword;
    }
}
