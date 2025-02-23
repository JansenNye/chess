package service;

import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.GameDAO;
import model.AuthData;
import model.GameData;
import service.requests.ListGamesRequest;
import service.results.ListGamesResult;

import java.util.ArrayList;
import java.util.List;

public class GameService {

    private final GameDAO gameDAO;
    private final AuthDAO authDAO;

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
            // Maps to HTTP 401 Unauthorized?
            throw new DataAccessException("Unauthorized: invalid or expired authToken");
        }

        // Fetch all games from GameDAO
        List<GameData> allGames = gameDAO.listGames();

        // Convert into the minimal output form (GameInfo)
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
}
