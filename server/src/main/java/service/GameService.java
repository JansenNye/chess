package service;
import chess.ChessGame;
import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.GameDAO;
import model.AuthData;
import model.GameData;
import service.requests.CreateGameRequest;
import service.requests.JoinGameRequest;
import service.requests.ListGamesRequest;
import service.results.CreateGameResult;
import service.results.JoinGameResult;
import service.results.ListGamesResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameService {

    private final GameDAO gameDAO;
    private final AuthDAO authDAO;
    private final Random idGenerator = new Random();

    public GameService(GameDAO gameDAO, AuthDAO authDAO) {
        this.gameDAO = gameDAO;
        this.authDAO = authDAO;
    }

    /**
     * Returns a list of all games, provided the user is authorized (authToken is valid).
     */
    public ListGamesResult listGames(ListGamesRequest request) throws DataAccessException {
        // Validate input
        if (request.authToken() == null || request.authToken().isBlank()) {
            throw new DataAccessException("Missing or invalid authToken");
        }

        // Check if authToken is valid
        AuthData authData = authDAO.getAuth(request.authToken());
        if (authData == null) {
            throw new DataAccessException("Unauthorized: invalid or expired authToken");
        }

        // Fetch all games from GameDAO
        List<GameData> allGames = gameDAO.listGames();

        // Convert each GameData to simpler output (GameInfo)
        List<ListGamesResult.GameInfo> gameInfos = new ArrayList<>();
        for (GameData g : allGames) {
            gameInfos.add(
                    new ListGamesResult.GameInfo(
                            g.gameID(),
                            g.whiteUsername(),
                            g.blackUsername(),
                            g.gameName()
                    )
            );
        }

        // Return result
        return new ListGamesResult(gameInfos);
    }

    /**
     * Creates a new game in the system, returning the new game’s ID.
     * We initialize a new ChessGame object so we actually store game state in the DB.
     */
    public CreateGameResult createGame(CreateGameRequest request) throws DataAccessException {
        // Validate request
        if (request.authToken() == null || request.authToken().isBlank()) {
            throw new DataAccessException("Missing auth token");
        }
        if (request.gameName() == null || request.gameName().isBlank()) {
            throw new DataAccessException("Missing or invalid gameName");
        }

        // Verify auth token
        AuthData authData = authDAO.getAuth(request.authToken());
        if (authData == null) {
            throw new DataAccessException("Unauthorized: invalid authToken");
        }

        int newID = Math.abs(idGenerator.nextInt());

        ChessGame newChessGame = new ChessGame();
        GameData newGame = new GameData(
                newID,
                null,
                null,
                request.gameName(),
                newChessGame
        );

        // Insert into DAO, return result
        gameDAO.createGame(newGame);
        return new CreateGameResult(newID);
    }

    /**
     * Joins the specified game as either WHITE or BLACK, if available.
     * Demonstrates how you'd retrieve the ChessGame and update it if needed.
     */
    public JoinGameResult joinGame(JoinGameRequest request) throws DataAccessException {
        // Validate request
        if (request.authToken() == null || request.authToken().isBlank()) {
            throw new DataAccessException("No auth token provided");
        }
        if (request.gameID() <= 0) {
            throw new DataAccessException("Invalid game ID");
        }
        if (!"WHITE".equalsIgnoreCase(request.playerColor()) &&
                !"BLACK".equalsIgnoreCase(request.playerColor())) {
            throw new DataAccessException("Invalid player color (must be WHITE or BLACK)");
        }

        // Check auth token
        AuthData authData = authDAO.getAuth(request.authToken());
        if (authData == null) {
            throw new DataAccessException("Unauthorized: invalid authToken");
        }

        // Fetch game from DAO
        GameData game = gameDAO.getGame(request.gameID());
        if (game == null) {
            throw new DataAccessException("Game not found with ID: " + request.gameID());
        }

        // Access current ChessGame state
        ChessGame chessGame = game.game();
        if (chessGame == null) {
            chessGame = new ChessGame();
        }

        // Determine which color the user wants to join
        String username = authData.username();
        if ("WHITE".equalsIgnoreCase(request.playerColor())) {
            if (game.whiteUsername() == null) {
                // Fill the white slot
                game = new GameData(
                        game.gameID(),
                        username,
                        game.blackUsername(),
                        game.gameName(),
                        chessGame
                );
            } else if (!game.whiteUsername().equals(username)) {
                throw new DataAccessException("White slot already taken");
            }
        } else { // "BLACK"
            if (game.blackUsername() == null) {
                // Fill black slot
                game = new GameData(
                        game.gameID(),
                        game.whiteUsername(),
                        username,
                        game.gameName(),
                        chessGame
                );
            } else if (!game.blackUsername().equals(username)) {
                throw new DataAccessException("Black slot already taken");
            }
        }
        gameDAO.updateGame(game);

        return new JoinGameResult();
    }
}
