package com.chess.demo.controller;

import com.chess.demo.dto.leaderboard.FideLeaderboardResponse;
import com.chess.demo.dto.leaderboard.LeaderboardEntryResponse;
import com.chess.demo.service.FideLeaderboardService;
import com.chess.demo.service.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final FideLeaderboardService fideLeaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService,
                                 FideLeaderboardService fideLeaderboardService) {
        this.leaderboardService = leaderboardService;
        this.fideLeaderboardService = fideLeaderboardService;
    }

    @GetMapping
    public ResponseEntity<List<LeaderboardEntryResponse>> getLeaderboard(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(leaderboardService.getLeaderboard(query));
    }

    @GetMapping("/fide")
    public ResponseEntity<FideLeaderboardResponse> getFideLeaderboard(@RequestParam(required = false) String query,
                                                                      @RequestParam(required = false) String timeControl,
                                                                      @RequestParam(required = false) String country,
                                                                      @RequestParam(required = false) String gender,
                                                                      @RequestParam(required = false) String division,
                                                                      @RequestParam(required = false) Integer page,
                                                                      @RequestParam(required = false) Integer size,
                                                                      @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(fideLeaderboardService.getLeaderboard(
                query,
                timeControl,
                country,
                gender,
                division,
                page,
                size,
                activeOnly
        ));
    }
}
