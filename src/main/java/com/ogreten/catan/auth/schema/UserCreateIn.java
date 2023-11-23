package com.ogreten.catan.auth.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateIn {
    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    @Schema(description = "Email of the user", example = "test@test.com")
    private String email;

    @NotBlank(message = "Password is mandatory")
    @Size(min = 8, max = 32, message = "Password should be at least 8 characters, at most 32 characters")
    @Schema(description = "Password of the user which is at least 8, at most 32 characters", example = "v3ryStr0ngP@ssw0rd")
    private String password;

    @Size(min = 3, max = 32, message = "First name should be between 3 and 32 characters")
    @NotBlank(message = "First name is mandatory")
    @Schema(description = "First name of the user", example = "Ahmet Can")
    private String firstName;

    @Size(min = 3, max = 32, message = "Last name should be between 3 and 32 characters")
    @NotBlank(message = "Last name is mandatory")
    @Schema(description = "Last name of the user", example = "Ogreten")
    private String lastName;
}
