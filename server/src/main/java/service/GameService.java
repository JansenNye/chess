package service;

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

        // Fetch all games from GameDAO, convert to the minimal output form (GameInfo)
        List<GameData> allGames = gameDAO.listGames();
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
        } // Return result
        return new ListGamesResult(gameInfos);
    }

    /**
     * Creates a new game in the system, returning the new game’s ID.
     */
    public CreateGameResult createGame(CreateGameRequest request) throws DataAccessException {
        // Validate request
        if (request.authToken() == null || request.authToken().isBlank()) {
            throw new DataAccessException("Missing auth token");
        } if (request.gameName() == null || request.gameName().isBlank()) {
            throw new DataAccessException("Missing or invalid gameName");
        }

        // Verify auth token
        AuthData authData = authDAO.getAuth(request.authToken());
        if (authData == null) {
            throw new DataAccessException("Unauthorized: invalid authToken");
        }

        // Generate a unique gameID
        int newID = Math.abs(idGenerator.nextInt());

        // Create a new GameData object
        GameData newGame = new GameData(
                newID,
                null,
                null,
                request.gameName(),
                null
        );

        // Insert into DAO, return result
        gameDAO.createGame(newGame);
        return new CreateGameResult(newID);
    }

    /**
     * Joins the specified game as either WHITE or BLACK, if available.
     */
    public JoinGameResult joinGame(JoinGameRequest request) throws DataAccessException {
        // Validate request
        if (request.authToken() == null || request.authToken().isBlank()) {
            throw new DataAccessException("No auth token provided");
        } if (request.gameID() <= 0) {
            throw new DataAccessException("Invalid game ID");
        } if (!"WHITE".equalsIgnoreCase(request.playerColor()) &&
                !"BLACK".equalsIgnoreCase(request.playerColor())) {
            throw new DataAccessException("Invalid player color (must be WHITE or BLACK)");
        }

        // Check auth token
        AuthData authData = authDAO.getAuth(request.authToken());
        if (authData == null) {
            throw new DataAccessException("Unauthorized: invalid authToken");
        }

        // Fetch game
        GameData game = gameDAO.getGame(request.gameID());
        if (game == null) {
            throw new DataAccessException("Game not found with ID: " + request.gameID());
        }

        // Check availability for requested color
        String username = authData.username();
        if (request.playerColor().equalsIgnoreCase("WHITE")) {
            if (game.whiteUsername() == null) {
                game = new GameData(
                        game.gameID(),
                        username,
                        game.blackUsername(),
                        game.gameName(),
                        game.game()
                );
            } else if (game.whiteUsername().equals(username)) {
                // Empty
            } else {
                throw new DataAccessException("White slot already taken");
            }
        } else { // BLACK
            if (game.blackUsername() == null) {
                game = new GameData(
                        game.gameID(),
                        game.whiteUsername(),
                        username,
                        game.gameName(),
                        game.game()
                );
            } else if (game.blackUsername().equals(username)) {
                // throw error?
            } else {
                throw new DataAccessException("Black slot already taken");
            }
        }

        gameDAO.updateGame(game);

        return new JoinGameResult();
    }
}
