package com.ogreten.catan.leaderboard.repository;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ogreten.catan.leaderboard.domain.UserEarnedPoints;
import com.ogreten.catan.leaderboard.schema.UserWithPoints;

public interface LeaderboardRepository extends JpaRepository<UserEarnedPoints, Integer> {

        @Query(value = """
                        SELECT new com.ogreten.catan.leaderboard.schema.UserWithPoints(new com.ogreten.catan.auth.domain.User(u.id, u.email, u.password, u.firstName, u.lastName), SUM(uep.points) as points)
                        FROM User u
                        JOIN UserEarnedPoints uep ON uep.user = u
                        WHERE uep.at >= ?1 AND uep.at <= ?2
                        GROUP BY u.id
                        ORDER BY SUM(uep.points) DESC
                        """)
        Page<UserWithPoints> getLeaderboardForCustomTimePeriod(Instant startDate,
                        Instant endDate, Pageable pageable);
}
