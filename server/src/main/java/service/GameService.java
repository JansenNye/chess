package service;

import chess.ChessGame;
import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.GameDAO;
import model.AuthData;
import model.GameData;
import model.GameStatus;
import requests.CreateGameRequest;
import requests.JoinGameRequest;
import requests.ListGamesRequest;
import results.CreateGameResult;
import results.JoinGameResult;
import results.ListGamesResult;

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

    public ListGamesResult listGames(ListGamesRequest request) throws DataAccessException {
        if (request == null || request.authToken() == null || request.authToken().isBlank()) {
            throw new DataAccessException("Error: bad request"); // 400
        }
        AuthData authData = authDAO.getAuth(request.authToken());
        if (authData == null) {
            throw new DataAccessException("Error: unauthorized"); // 401
        }
        List<GameData> allGames = gameDAO.listGames();
        List<ListGamesResult.GameInfo> gameInfos = new ArrayList<>();
        if (allGames != null) {
            for (GameData g : allGames) {
                gameInfos.add(new ListGamesResult.GameInfo(g.gameID(), g.whiteUsername(), g.blackUsername(), g.gameName()));
            }
        }
        return new ListGamesResult(gameInfos);
    }

    public CreateGameResult createGame(CreateGameRequest request) throws DataAccessException {
        if (request == null) {
            throw new DataAccessException("Error: bad request"); // 400
        }
        if (request.authToken() == null || request.authToken().isBlank()) {
            throw new DataAccessException("Error: bad request"); // 400
        }
        if (request.gameName() == null || request.gameName().isBlank()) {
            throw new DataAccessException("Error: bad request"); // 400
        }
        AuthData authData = authDAO.getAuth(request.authToken());
        if (authData == null) {
            throw new DataAccessException("Error: unauthorized"); // 401
        }
        int newID = Math.abs(idGenerator.nextInt());
        ChessGame newChessGame = new ChessGame();
        newChessGame.getBoard().resetBoard();
        GameData newGame = new GameData(newID, null, null, request.gameName(), newChessGame, GameStatus.ACTIVE);
        try {
            gameDAO.createGame(newGame);
        } catch (DataAccessException e) {
            System.err.println("DAO Error during createGame: " + e.getMessage());
            throw new DataAccessException("Error: Failed to create game in storage"); // 500
        }
        return new CreateGameResult(newID);
    }

    public JoinGameResult joinGame(JoinGameRequest request) throws DataAccessException {
        // 1. Validate request object itself
        if (request == null) {
            throw new DataAccessException("Error: bad request"); // 400
        }
        // 2. Validate authToken
        if (request.authToken() == null || request.authToken().isBlank()) {
            throw new DataAccessException("Error: bad request"); // 400 initially, mapped to 401 later if token invalid
        }
        // 3. Validate gameID format
        if (request.gameID() <= 0) {
            throw new DataAccessException("Error: bad request"); // 400
        }

        // 4. Validate playerColor
        String requestedColor = request.playerColor(); // Can be null or empty if observing
        boolean isObserving = (requestedColor == null || requestedColor.isBlank());
        boolean isValidPlayerColor = !isObserving && (requestedColor.equalsIgnoreCase("WHITE") || requestedColor.equalsIgnoreCase("BLACK"));

        // If a color was specified, but it wasn't valid ("WHITE" or "BLACK")
        if (!isObserving && !isValidPlayerColor) {
            throw new DataAccessException("Error: bad request"); // 400 - Explicitly reject invalid colors
        }

        // 5. Now validate the auth token's existence
        AuthData authData = authDAO.getAuth(request.authToken());
        if (authData == null) {
            throw new DataAccessException("Error: unauthorized"); // 401
        }

        // 6. Fetch game from DAO (check if game exists)
        GameData game = gameDAO.getGame(request.gameID());
        if (game == null) {
            throw new DataAccessException("Error: bad request"); // 400 (Game not found for the given ID)
        }

        // 7. If joining as a player (color was valid and provided)
        if (isValidPlayerColor) {
            String username = authData.username();
            GameData updatedGame = game; // Start with current game data

            if ("WHITE".equalsIgnoreCase(requestedColor)) {
                if (game.whiteUsername() == null) {
                    // Assign user to white
                    updatedGame = new GameData(game.gameID(), username, game.blackUsername(), game.gameName(), game.game(), game.status());
                } else if (!game.whiteUsername().equals(username)) {
                    // White slot is taken by someone else
                    throw new DataAccessException("Error: already taken"); // 403
                } // If already joined as white, updatedGame == game, no change needed
            } else { // "BLACK"
                if (game.blackUsername() == null) {
                    // Assign user to black
                    updatedGame = new GameData(game.gameID(), game.whiteUsername(), username, game.gameName(), game.game(), game.status());
                } else if (!game.blackUsername().equals(username)) {
                    // Black slot is taken by someone else
                    throw new DataAccessException("Error: already taken"); // 403
                }
            }

            // Only call DAO update if game state actually changed
            if (updatedGame != game) {
                try {
                    gameDAO.updateGame(updatedGame);
                } catch (DataAccessException e) {
                    System.err.println("DAO Error during joinGame update: " + e.getMessage());
                    throw new DataAccessException("Error: Failed to update game during join"); // 500
                }
            }
        }
        // 8. If observing (isObserving was true), no game update is needed.

        return new JoinGameResult();
    }

    
    public GameData getGameById(int gameID) throws DataAccessException {
        if (gameID <= 0) {
            throw new DataAccessException("Error: bad request"); // 400
        }

        GameData game = gameDAO.getGame(gameID);
        if (game == null) {
            throw new DataAccessException("Error: bad request"); // 400
        }

        return game;
    }
}
