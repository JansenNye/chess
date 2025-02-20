package service;

import dataaccess.*;
import model.AuthData;
import model.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.requests.ListGamesRequest;
import service.results.ListGamesResult;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTest {

    private GameDAO gameDAO;
    private AuthDAO authDAO;
    private GameService gameService;

    @BeforeEach
    void setup() {
        gameDAO = new MemoryGameDAO();
        authDAO = new MemoryAuthDAO();
        gameService = new GameService(gameDAO, authDAO);
    }

    @Test
    void testListGames_Success() throws DataAccessException {
        // Create valid auth token
        AuthData goodToken = new AuthData("valid_token", "alice");
        authDAO.createAuth(goodToken);

        // Add games
        GameData game1 = new GameData(101, "alice", null, "MyFirstGame", null);
        GameData game2 = new GameData(202, "bob", "charlie", "Showdown", null);
        gameDAO.createGame(game1);
        gameDAO.createGame(game2);

        // Make request
        ListGamesRequest request = new ListGamesRequest("valid_token");

        // Call service
        ListGamesResult result = gameService.listGames(request);

        // Check result
        assertNotNull(result);
        assertNotNull(result.games());
        assertEquals(2, result.games().size());

        // Confirm the data matches
        ListGamesResult.GameInfo first = result.games().getFirst();
        assertEquals(101, first.gameID());
        assertEquals("alice", first.whiteUsername());
        assertNull(first.blackUsername());
        assertEquals("MyFirstGame", first.gameName());
    }

    @Test
    void testListGames_InvalidToken() throws DataAccessException {
        // Insert valid token for someone else, but request will pass "bogus_token"
        AuthData goodToken = new AuthData("real_token", "someone");
        authDAO.createAuth(goodToken);

        ListGamesRequest badReq = new ListGamesRequest("bogus_token");
        assertThrows(DataAccessException.class, () -> gameService.listGames(badReq));
    }

    @Test
    void testListGames_MissingToken() {
        // Null or empty token => should fail
        ListGamesRequest missing = new ListGamesRequest("");
        assertThrows(DataAccessException.class, () -> gameService.listGames(missing));
    }

    @Test
    void testListGames_EmptyList() throws DataAccessException {
        //We have valid token, just no games in DAO
        authDAO.createAuth(new AuthData("valid_token", "bob"));

        // Request
        ListGamesRequest request = new ListGamesRequest("valid_token");
        ListGamesResult result = gameService.listGames(request);

        // Should return empty array
        assertNotNull(result);
        assertEquals(0, result.games().size());
    }
}
