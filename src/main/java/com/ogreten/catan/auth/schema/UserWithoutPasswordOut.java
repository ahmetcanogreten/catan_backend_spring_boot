package com.ogreten.catan.auth.schema;

import java.util.UUID;

import com.ogreten.catan.auth.domain.User;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "UserWithoutPassword", description = "User information without password field")
public class UserWithoutPasswordOut {

    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Email of the user", example = "test@test.com")
    private String email;

    @Schema(description = "First name of the user", example = "Ahmet Can")
    private String firstName;

    @Schema(description = "Last name of the user", example = "Ogreten")
    private String lastName;

    public static UserWithoutPasswordOut fromUser(User user) {
        UserWithoutPasswordOut userWithoutPassword = new UserWithoutPasswordOut();
        userWithoutPassword.setId(user.getId());
        userWithoutPassword.setEmail(user.getEmail());
        userWithoutPassword.setFirstName(user.getFirstName());
        userWithoutPassword.setLastName(user.getLastName());
        return userWithoutPassword;
    }
}
