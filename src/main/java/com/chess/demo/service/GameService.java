package com.chess.demo.service;

import com.chess.demo.common.ApiException;
import com.chess.demo.dto.game.CreateGameRequest;
import com.chess.demo.dto.game.GameResponse;
import com.chess.demo.dto.game.MoveRequest;
import com.chess.demo.dto.analysis.GameAnalysisResponse;
import com.chess.demo.entity.Game;
import com.chess.demo.entity.GameMove;
import com.chess.demo.entity.User;
import com.chess.demo.repository.GameMoveRepository;
import com.chess.demo.repository.GameRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GameService {

    private final GameRepository gameRepository;
    private final GameMoveRepository gameMoveRepository;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChessEngineService chessEngineService;
    private final PostGameAnalysisService postGameAnalysisService;

    public GameService(GameRepository gameRepository,
                       GameMoveRepository gameMoveRepository,
                       UserMapper userMapper,
                       ObjectMapper objectMapper,
                       SimpMessagingTemplate messagingTemplate,
                       ChessEngineService chessEngineService,
                       PostGameAnalysisService postGameAnalysisService) {
        this.gameRepository = gameRepository;
        this.gameMoveRepository = gameMoveRepository;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
        this.chessEngineService = chessEngineService;
        this.postGameAnalysisService = postGameAnalysisService;
    }

    @Transactional
    public GameResponse createGame(User user, CreateGameRequest request) {
        Game game = new Game();
        String colorPreference = request.colorPreference() == null ? "random" : request.colorPreference().trim().toLowerCase();
        String timeControl = request.timeControl() == null || request.timeControl().isBlank()
                ? user.getSettings().getDefaultTimeControl()
                : request.timeControl();
        configureGame(game, timeControl, Boolean.TRUE.equals(request.rated()));
        if ("black".equals(colorPreference)) {
            game.setBlackPlayer(user);
        } else {
            game.setWhitePlayer(user);
        }
        if ("random".equals(colorPreference)) {
            game.setWhitePlayer(user);
        }

        return toResponse(gameRepository.save(game));
    }

    @Transactional
    public GameResponse createMatchedGame(User whitePlayer, User blackPlayer, String timeControl, boolean rated) {
        Game game = new Game();
        configureGame(game, timeControl, rated);
        game.setWhitePlayer(whitePlayer);
        game.setBlackPlayer(blackPlayer);
        return toResponse(gameRepository.save(game));
    }

    @Transactional(readOnly = true)
    public GameResponse getGame(UUID gameId, User user) {
        Game game = getGameEntity(gameId);
        ensureParticipant(game, user);
        return toResponse(game);
    }

    @Transactional
    public GameResponse submitMove(UUID gameId, User user, MoveRequest request) {
        Game game = getGameEntity(gameId);
        ensureParticipant(game, user);
        ensureGameActive(game);
        String expectedTurn = game.getTurnColor();
        if (Boolean.TRUE.equals(game.getBotGame())) {
            ensureHumansTurnInBotGame(game, user, expectedTurn);
        } else if (!isPlayersTurn(game, user, expectedTurn)) {
            throw new ApiException(HttpStatus.CONFLICT, "not_your_turn");
        }

        String from = normalizeSquare(request.from());
        String to = normalizeSquare(request.to());
        String promotion = normalizePromotion(request.promotion());
        String fenAfter = buildPlaceholderFen(game, from, to, promotion);
        if (Boolean.TRUE.equals(game.getBotGame())) {
            ChessEngineService.PositionInfo positionInfo = chessEngineService.describePosition(buildUciMoves(game));
            String uciMove = resolveLegalUciMove(positionInfo.legalMoves(), from, to, promotion);
            if (uciMove == null) {
                if (isDuplicateBotMoveSubmission(game, user, toUciMove(from, to, promotion))) {
                    return toResponse(game);
                }
                throw new ApiException(HttpStatus.BAD_REQUEST, "illegal_move");
            }
            promotion = extractPromotion(uciMove);
            fenAfter = chessEngineService.describePosition(appendUciMove(game, uciMove)).fen();
        }

        applyMove(game, user, expectedTurn, from, to, promotion, fenAfter);
        if (Boolean.TRUE.equals(game.getBotGame())) {
            runBotTurnIfNeeded(game, user);
        }
        GameResponse response = toResponse(gameRepository.save(game));
        broadcastGameState(response);
        return response;
    }

    @Transactional
    public GameResponse runBotTurnIfNeeded(UUID gameId, User user) {
        Game game = getGameEntity(gameId);
        ensureParticipant(game, user);
        ensureGameActive(game);
        runBotTurnIfNeeded(game, user);
        return toResponse(gameRepository.save(game));
    }

    @Transactional
    public GameResponse resign(UUID gameId, User user) {
        Game game = getGameEntity(gameId);
        ensureParticipant(game, user);
        ensureGameActive(game);
        boolean userIsWhite = game.getWhitePlayer() != null && game.getWhitePlayer().getId().equals(user.getId());
        game.setStatus("FINISHED");
        game.setResult(userIsWhite ? "BLACK_WIN" : "WHITE_WIN");
        game.setResultReason("RESIGNATION");
        game.setEndedAt(Instant.now());
        GameResponse response = toResponse(gameRepository.save(game));
        broadcastGameState(response);
        return response;
    }

    @Transactional
    public GameResponse offerDraw(UUID gameId, User user) {
        Game game = getGameEntity(gameId);
        ensureParticipant(game, user);
        ensureGameActive(game);
        game.setDrawOfferedBy(user.getId());
        GameResponse response = toResponse(gameRepository.save(game));
        broadcastGameState(response);
        return response;
    }

    @Transactional
    public GameResponse respondToDraw(UUID gameId, User user, boolean accepted) {
        Game game = getGameEntity(gameId);
        ensureParticipant(game, user);
        ensureGameActive(game);
        if (game.getDrawOfferedBy() == null || game.getDrawOfferedBy().equals(user.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "draw_offer_not_available");
        }
        if (accepted) {
            game.setStatus("FINISHED");
            game.setResult("DRAW");
            game.setResultReason("DRAW_AGREED");
            game.setEndedAt(Instant.now());
        }
        game.setDrawOfferedBy(null);
        GameResponse response = toResponse(gameRepository.save(game));
        broadcastGameState(response);
        return response;
    }

    @Transactional(readOnly = true)
    public String getPgn(UUID gameId, User user) {
        Game game = getGameEntity(gameId);
        ensureParticipant(game, user);
        return game.getPgn();
    }

    @Transactional(readOnly = true)
    public String getFen(UUID gameId, User user) {
        Game game = getGameEntity(gameId);
        ensureParticipant(game, user);
        return game.getFen();
    }

    @Transactional(readOnly = true)
    public GameAnalysisResponse analyzeGame(UUID gameId, User user) {
        Game game = getGameEntity(gameId);
        ensureParticipant(game, user);
        return postGameAnalysisService.analyzeGame(game, user, gameMoveRepository.findByGameOrderByMoveNumberAsc(game));
    }

    @Transactional(readOnly = true)
    public Game getGameEntity(UUID gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "game_not_found"));
    }

    @Transactional(readOnly = true)
    public GameResponse toResponse(Game game) {
        return userMapper.toGameResponse(game, readHistory(game));
    }

    public void broadcastGameState(GameResponse response) {
        messagingTemplate.convertAndSend("/topic/game/" + response.gameId(), response);
    }

    private void ensureParticipant(Game game, User user) {
        boolean isWhite = game.getWhitePlayer() != null && game.getWhitePlayer().getId().equals(user.getId());
        boolean isBlack = game.getBlackPlayer() != null && game.getBlackPlayer().getId().equals(user.getId());
        if (!isWhite && !isBlack) {
            throw new ApiException(HttpStatus.FORBIDDEN, "not_game_participant");
        }
    }

    private void ensureGameActive(Game game) {
        if (!"ACTIVE".equals(game.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "game_not_active");
        }
    }

    private void ensureHumansTurnInBotGame(Game game, User user, String expectedTurn) {
        boolean userIsWhite = game.getWhitePlayer() != null && game.getWhitePlayer().getId().equals(user.getId());
        boolean userIsBlack = game.getBlackPlayer() != null && game.getBlackPlayer().getId().equals(user.getId());
        boolean isHumansTurn = ("white".equals(expectedTurn) && userIsWhite)
                || ("black".equals(expectedTurn) && userIsBlack);
        if (!isHumansTurn) {
            throw new ApiException(HttpStatus.CONFLICT, "not_your_turn");
        }
    }

    private boolean isPlayersTurn(Game game, User user, String expectedTurn) {
        if ("white".equals(expectedTurn)) {
            return game.getWhitePlayer() != null && game.getWhitePlayer().getId().equals(user.getId());
        }
        return game.getBlackPlayer() != null && game.getBlackPlayer().getId().equals(user.getId());
    }

    private String normalizeSquare(String square) {
        String value = square == null ? "" : square.trim().toLowerCase();
        if (!value.matches("^[a-h][1-8]$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_square");
        }
        return value;
    }

    private String normalizePromotion(String promotion) {
        if (promotion == null) {
            return null;
        }
        String value = promotion.trim().toLowerCase();
        if (value.isBlank()) {
            return null;
        }
        if (!value.matches("^[qrbn]$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_promotion");
        }
        return value;
    }

    private String buildSan(String from, String to, String promotion) {
        return promotion == null || promotion.isBlank() ? from + "-" + to : from + "-" + to + "=" + promotion;
    }

    private void applyMove(Game game,
                           User actor,
                           String expectedTurn,
                           String from,
                           String to,
                           String promotion,
                           String fenAfter) {
        String san = buildSan(from, to, promotion);

        GameMove move = new GameMove();
        move.setGame(game);
        move.setPlayer(actor);
        move.setMoveNumber((int) (gameMoveRepository.countByGame(game) + 1));
        move.setMoveColor(expectedTurn);
        move.setFromSquare(from);
        move.setToSquare(to);
        move.setPromotion(promotion);
        move.setSan(san);
        move.setFenAfter(fenAfter);
        gameMoveRepository.save(move);

        List<String> history = readHistory(game);
        history.add(san);
        game.setHistoryJson(writeHistory(history));
        game.setPgn(String.join(" ", history));
        game.setFen(fenAfter);
        game.setLastMoveFrom(from);
        game.setLastMoveTo(to);
        game.setLastMoveSan(san);
        game.setDrawOfferedBy(null);
        game.setTurnColor("white".equals(expectedTurn) ? "black" : "white");
    }

    private void runBotTurnIfNeeded(Game game, User user) {
        if (!Boolean.TRUE.equals(game.getBotGame()) || !"ACTIVE".equals(game.getStatus())) {
            return;
        }

        String expectedTurn = game.getTurnColor();
        boolean userIsWhite = game.getWhitePlayer() != null && game.getWhitePlayer().getId().equals(user.getId());
        boolean userIsBlack = game.getBlackPlayer() != null && game.getBlackPlayer().getId().equals(user.getId());
        boolean isHumansTurn = ("white".equals(expectedTurn) && userIsWhite)
                || ("black".equals(expectedTurn) && userIsBlack);
        if (isHumansTurn) {
            return;
        }

        List<String> moves = buildUciMoves(game);
        String bestMove = chessEngineService.findBestMove(moves, game.getBotLevel() == null ? 1 : game.getBotLevel());
        if (bestMove == null || bestMove.length() < 4) {
            return;
        }

        String from = bestMove.substring(0, 2);
        String to = bestMove.substring(2, 4);
        String promotion = bestMove.length() > 4 ? bestMove.substring(4) : null;
        String fenAfter = chessEngineService.describePosition(appendUciMove(game, bestMove)).fen();
        applyMove(game, user, expectedTurn, from, to, promotion, fenAfter);
    }

    private List<String> buildUciMoves(Game game) {
        List<String> moves = new ArrayList<>();
        for (GameMove move : gameMoveRepository.findByGameOrderByMoveNumberAsc(game)) {
            moves.add(toUciMove(move.getFromSquare(), move.getToSquare(), move.getPromotion()));
        }
        return moves;
    }

    private boolean isDuplicateBotMoveSubmission(Game game, User user, String uciMove) {
        if (!Boolean.TRUE.equals(game.getBotGame())) {
            return false;
        }

        List<GameMove> moves = gameMoveRepository.findByGameOrderByMoveNumberAsc(game);
        if (moves.size() < 2) {
            return false;
        }

        GameMove botReply = moves.get(moves.size() - 1);
        GameMove priorHumanMove = moves.get(moves.size() - 2);
        if (priorHumanMove.getPlayer() == null || !priorHumanMove.getPlayer().getId().equals(user.getId())) {
            return false;
        }

        if (priorHumanMove.getMoveColor() == null
                || botReply.getMoveColor() == null
                || !priorHumanMove.getMoveColor().equals(game.getTurnColor())
                || priorHumanMove.getMoveColor().equals(botReply.getMoveColor())) {
            return false;
        }

        return toUciMove(priorHumanMove.getFromSquare(), priorHumanMove.getToSquare(), priorHumanMove.getPromotion())
                .equals(uciMove);
    }

    private String resolveLegalUciMove(java.util.Set<String> legalMoves, String from, String to, String promotion) {
        String baseMove = from + to;
        String promotedMove = toUciMove(from, to, promotion);
        if (promotion != null && legalMoves.contains(promotedMove)) {
            return promotedMove;
        }
        if (legalMoves.contains(baseMove)) {
            return baseMove;
        }
        return null;
    }

    private String extractPromotion(String uciMove) {
        return uciMove.length() > 4 ? uciMove.substring(4) : null;
    }

    private List<String> appendUciMove(Game game, String uciMove) {
        List<String> moves = buildUciMoves(game);
        moves.add(uciMove);
        return moves;
    }

    private String toUciMove(String from, String to, String promotion) {
        String suffix = promotion == null || promotion.isBlank() ? "" : promotion;
        return from + to + suffix;
    }

    private String buildPlaceholderFen(Game game, String from, String to, String promotion) {
        String suffix = promotion == null || promotion.isBlank() ? "" : ":" + promotion;
        return "move:" + (game.getMoves().size() + 1) + ":" + from + ":" + to + suffix;
    }

    private List<String> readHistory(Game game) {
        try {
            return objectMapper.readValue(game.getHistoryJson(), new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            return new ArrayList<>();
        }
    }

    private String writeHistory(List<String> history) {
        try {
            return objectMapper.writeValueAsString(history);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "history_serialization_failed");
        }
    }

    private int parseStartingSeconds(String timeControl) {
        String[] parts = timeControl.split("\\+");
        try {
            int minutes = Integer.parseInt(parts[0]);
            return minutes * 60;
        } catch (Exception exception) {
            return 600;
        }
    }

    private void configureGame(Game game, String timeControl, boolean rated) {
        String requestedTimeControl = timeControl == null || timeControl.isBlank() ? "10+0" : timeControl.trim();
        game.setTimeControl(requestedTimeControl);
        int startingTime = parseStartingSeconds(requestedTimeControl);
        game.setWhiteTimeRemaining(startingTime);
        game.setBlackTimeRemaining(startingTime);
        game.setRated(rated);
    }
}
