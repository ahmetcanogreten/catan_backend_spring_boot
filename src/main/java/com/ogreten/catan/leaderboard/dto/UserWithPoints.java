package com.ogreten.catan.leaderboard.dto;

import com.ogreten.catan.auth.domain.User;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserWithPoints {
    User user;
    double points;

}
