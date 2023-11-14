package com.ogreten.catan.leaderboard.controller;

import java.time.Instant;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ogreten.catan.leaderboard.dto.UserWithPoints;
import com.ogreten.catan.leaderboard.repository.LeaderboardRepository;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    LeaderboardRepository leaderboardRepository;

    public LeaderboardController(LeaderboardRepository leaderboardRepository) {
        this.leaderboardRepository = leaderboardRepository;
    }

    @GetMapping("")
    public Page<UserWithPoints> getLeaderboardForCustomTimePeriod(
            @RequestParam Instant startDate,
            @RequestParam Instant endDate,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {

        Pageable pageable = PageRequest.of(pageNo, pageSize);
        return leaderboardRepository.getLeaderboardForCustomTimePeriod(startDate, endDate, pageable);
    }

}
