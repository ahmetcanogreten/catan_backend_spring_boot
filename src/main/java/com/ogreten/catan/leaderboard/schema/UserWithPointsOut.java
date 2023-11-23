package com.ogreten.catan.leaderboard.schema;

import com.ogreten.catan.auth.schema.UserWithoutPasswordOut;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserWithPointsOut {
    @Schema(description = "User information without password field")
    UserWithoutPasswordOut user;

    @Schema(description = "Total points of the user in specified range", example = "100")
    double points;
}
