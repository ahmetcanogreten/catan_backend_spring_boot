package com.ogreten.catan.leaderboard.controller;

import java.time.Instant;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ogreten.catan.leaderboard.repository.LeaderboardRepository;
import com.ogreten.catan.leaderboard.schema.UserWithPointsOut;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;

@Transactional
@Tag(name = "Leaderboard", description = "Leaderboard Management API")
@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    LeaderboardRepository leaderboardRepository;

    public LeaderboardController(LeaderboardRepository leaderboardRepository) {
        this.leaderboardRepository = leaderboardRepository;
    }

    @Operation(summary = "Get leaderboard", description = "Get leaderboard between start and end date.", tags = {
            "leaderboard", "get" })
    @ApiResponse(responseCode = "200", content = {
            @Content(schema = @Schema(implementation = Page.class), mediaType = "application/json") })
    @GetMapping("")
    public Page<UserWithPointsOut> getLeaderboardForCustomTimePeriod(

            @Parameter(description = "Start date of the leaderboard.", example = "2020-01-01T00:00:00.000Z") @RequestParam Instant startDate,
            @Parameter(description = "End date of the leaderboard.", example = "2025-01-01T00:00:00.000Z") @RequestParam Instant endDate,
            @Parameter(description = "Page of the leaderboard.", example = "0") @RequestParam(defaultValue = "0") int pageNo,
            @Parameter(description = "Page size of the leaderboard.", example = "10") @RequestParam(defaultValue = "10") int pageSize) {

        Pageable pageable = PageRequest.of(pageNo, pageSize);
        return leaderboardRepository.getLeaderboardForCustomTimePeriod(startDate, endDate, pageable);
    }

}
