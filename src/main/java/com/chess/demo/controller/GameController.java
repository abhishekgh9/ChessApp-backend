package com.chess.demo.controller;

import com.chess.demo.dto.game.CreateGameRequest;
import com.chess.demo.dto.game.DrawResponseRequest;
import com.chess.demo.dto.game.GameResponse;
import com.chess.demo.dto.game.MoveRequest;
import com.chess.demo.dto.analysis.GameAnalysisResponse;
import com.chess.demo.service.CurrentUserService;
import com.chess.demo.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;
    private final CurrentUserService currentUserService;

    public GameController(GameService gameService, CurrentUserService currentUserService) {
        this.gameService = gameService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<GameResponse> createGame(Principal principal,
                                                   @RequestBody CreateGameRequest request) {
        return ResponseEntity.ok(gameService.createGame(currentUserService.requireUser(principal), request));
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameResponse> getGame(@PathVariable UUID gameId, Principal principal) {
        return ResponseEntity.ok(gameService.getGame(gameId, currentUserService.requireUser(principal)));
    }

    @PostMapping("/{gameId}/move")
    public ResponseEntity<GameResponse> submitMove(@PathVariable UUID gameId,
                                                   Principal principal,
                                                   @Valid @RequestBody MoveRequest request) {
        return ResponseEntity.ok(gameService.submitMove(gameId, currentUserService.requireUser(principal), request));
    }

    @PostMapping("/{gameId}/resign")
    public ResponseEntity<GameResponse> resign(@PathVariable UUID gameId, Principal principal) {
        return ResponseEntity.ok(gameService.resign(gameId, currentUserService.requireUser(principal)));
    }

    @PostMapping("/{gameId}/draw-offer")
    public ResponseEntity<GameResponse> offerDraw(@PathVariable UUID gameId, Principal principal) {
        return ResponseEntity.ok(gameService.offerDraw(gameId, currentUserService.requireUser(principal)));
    }

    @PostMapping("/{gameId}/draw-respond")
    public ResponseEntity<GameResponse> respondToDraw(@PathVariable UUID gameId,
                                                      Principal principal,
                                                      @RequestBody DrawResponseRequest request) {
        return ResponseEntity.ok(gameService.respondToDraw(gameId, currentUserService.requireUser(principal), Boolean.TRUE.equals(request.accepted())));
    }

    @GetMapping("/{gameId}/pgn")
    public ResponseEntity<Map<String, String>> getPgn(@PathVariable UUID gameId, Principal principal) {
        return ResponseEntity.ok(Map.of("pgn", gameService.getPgn(gameId, currentUserService.requireUser(principal))));
    }

    @GetMapping("/{gameId}/fen")
    public ResponseEntity<Map<String, String>> getFen(@PathVariable UUID gameId, Principal principal) {
        return ResponseEntity.ok(Map.of("fen", gameService.getFen(gameId, currentUserService.requireUser(principal))));
    }

    @GetMapping("/{gameId}/analysis")
    public ResponseEntity<GameAnalysisResponse> analyzeGame(@PathVariable UUID gameId, Principal principal) {
        return ResponseEntity.ok(gameService.analyzeGame(gameId, currentUserService.requireUser(principal)));
    }
}
