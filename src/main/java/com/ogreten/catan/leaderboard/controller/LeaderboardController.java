package com.ogreten.catan.leaderboard.controller;

import java.time.Instant;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ogreten.catan.game.domain.Game;
import com.ogreten.catan.leaderboard.repository.LeaderboardRepository;
import com.ogreten.catan.leaderboard.schema.UserWithPoints;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

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
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = {
                    @Content(schema = @Schema(implementation = Page.class), mediaType = "application/json") }),
    })
    @GetMapping("")
    public Page<UserWithPoints> getLeaderboardForCustomTimePeriod(

            @Parameter(description = "Start date of the leaderboard.") @RequestParam Instant startDate,
            @Parameter(description = "End date of the leaderboard.") @RequestParam Instant endDate,
            @Parameter(description = "Page of the leaderboard.") @RequestParam(defaultValue = "0") int pageNo,
            @Parameter(description = "Page size of the leaderboard.") @RequestParam(defaultValue = "10") int pageSize) {

        Pageable pageable = PageRequest.of(pageNo, pageSize);
        return leaderboardRepository.getLeaderboardForCustomTimePeriod(startDate, endDate, pageable);
    }

}
