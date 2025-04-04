package service;

import dataaccess.*;
import model.AuthData;
import model.GameData;
import model.GameStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import requests.CreateGameRequest;
import requests.JoinGameRequest;
import requests.ListGamesRequest;
import results.CreateGameResult;
import results.JoinGameResult;
import results.ListGamesResult;

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
    void testListGamesSuccess() throws DataAccessException {
        // Create valid auth token
        AuthData goodToken = new AuthData("valid_token", "alice");
        authDAO.createAuth(goodToken);

        // Add games
        GameData game1 = new GameData(101, "alice", null, "MyFirstGame", null, GameStatus.ACTIVE);
        GameData game2 = new GameData(202, "bob", "charlie", "Showdown", null, GameStatus.ACTIVE);
        gameDAO.createGame(game1);
        gameDAO.createGame(game2);

        // Make request, call service, check result
        ListGamesRequest request = new ListGamesRequest("valid_token");
        ListGamesResult result = gameService.listGames(request);
        assertNotNull(result);
        assertNotNull(result.games());
        assertEquals(2, result.games().size());

        // Confirm data matches
        ListGamesResult.GameInfo first = result.games().getFirst();
        assertEquals(101, first.gameID());
        assertEquals("alice", first.whiteUsername());
        assertNull(first.blackUsername());
        assertEquals("MyFirstGame", first.gameName());
    }

    @Test
    void testListGamesInvalidToken() throws DataAccessException {
        // Insert valid token for someone else, request will pass "bogus_token"
        AuthData goodToken = new AuthData("realtoken", "someone");
        authDAO.createAuth(goodToken);
        ListGamesRequest badReq = new ListGamesRequest("bogus_token");
        assertThrows(DataAccessException.class, () -> gameService.listGames(badReq));
    }

    @Test
    void testListGamesMissingToken() {
        // Null/empty token
        ListGamesRequest missing = new ListGamesRequest("");
        assertThrows(DataAccessException.class, () -> gameService.listGames(missing));
    }

    @Test
    void testListGamesEmptyList() throws DataAccessException {
        // Valid token, no games in DAO
        authDAO.createAuth(new AuthData("valid_token", "bob"));

        // Request
        ListGamesRequest request = new ListGamesRequest("valid_token");
        ListGamesResult result = gameService.listGames(request);

        // Should return empty array
        assertNotNull(result);
        assertEquals(0, result.games().size());
    }

    @Test
    void testJoinGameWhiteSuccess() throws DataAccessException {
        // Valid auth
        AuthData auth = new AuthData("tokentoken", "bigal");
        authDAO.createAuth(auth);

        // Create game with no players
        GameData newGame = new GameData(1001, null, null, "TestGame", null,  GameStatus.ACTIVE);
        gameDAO.createGame(newGame);

        // Make request
        JoinGameRequest request = new JoinGameRequest("tokentoken", "WHITE", 1001);

        // Call joinGame
        JoinGameResult result = gameService.joinGame(request);
        assertNotNull(result);

        // Verify game was updated
        GameData updatedGame = gameDAO.getGame(1001);
        assertEquals("bigal", updatedGame.whiteUsername());
        assertNull(updatedGame.blackUsername());
    }

    @Test
    void testJoinGameBlackSuccess() throws DataAccessException {
        // Similar but fill blackUsername
        authDAO.createAuth(new AuthData("token_1", "bobert"));
        gameDAO.createGame(new GameData(2002, null, null, "AnotherGame", null, GameStatus.ACTIVE));

        // Join game
        JoinGameRequest request = new JoinGameRequest("token_1", "BLACK", 2002);
        gameService.joinGame(request);
        GameData updated = gameDAO.getGame(2002);
        assertNull(updated.whiteUsername());
        assertEquals("bobert", updated.blackUsername());
    }

    @Test
    void testJoinGameInvalidToken() throws DataAccessException {
        // No tokens in DAO - invalid
        gameDAO.createGame(new GameData(3003, null, null, "GameNoAuth", null, GameStatus.ACTIVE));

        // Try to join game
        JoinGameRequest req = new JoinGameRequest("bogus", "WHITE", 3003);
        assertThrows(DataAccessException.class, () -> gameService.joinGame(req));
    }

    @Test
    void testJoinGameGameNotFound() throws DataAccessException {
        // Create auth
        authDAO.createAuth(new AuthData("token789", "charlie"));

        // No game with ID=9999
        JoinGameRequest req = new JoinGameRequest("token789", "WHITE", 9999);
        assertThrows(DataAccessException.class, () -> gameService.joinGame(req));
    }

    @Test
    void testJoinGameColorAlreadyTaken() throws DataAccessException {
        authDAO.createAuth(new AuthData("tokenD", "dave"));
        // Pre-insert game - already has Eve as white
        gameDAO.createGame(new GameData(4004, "eve", null, "FullWhiteSlot", null,  GameStatus.ACTIVE));

        // Now dave tries to join as white - not available
        JoinGameRequest req = new JoinGameRequest("tokenD", "WHITE", 4004);
        assertThrows(DataAccessException.class, () -> gameService.joinGame(req));
    }

    @Test
    void testJoinGameAlreadyJoined() throws DataAccessException {
        // Eve already in black slot
        authDAO.createAuth(new AuthData("tokenE", "eve"));
        gameDAO.createGame(new GameData(5005, null, "eve", "BlackSlotFull", null,  GameStatus.ACTIVE));

        JoinGameRequest req = new JoinGameRequest("tokenE", "BLACK", 5005);
        assertDoesNotThrow(() -> gameService.joinGame(req));

        // Check that blackUsername remains "eve"
        GameData updated = gameDAO.getGame(5005);
        assertEquals("eve", updated.blackUsername());
    }
    @Test
    void testCreateGameSuccess() throws DataAccessException {
        // Insert valid token to authDAO
        AuthData validAuth = new AuthData("good_token", "alice");
        authDAO.createAuth(validAuth);

        // Build request w/a valid token and gameName
        CreateGameRequest request = new CreateGameRequest("good_token", "MyCoolGame");

        // Call createGame, check that result is valid
        CreateGameResult result = gameService.createGame(request);
        assertNotNull(result, "Should not be null on success");
        assertTrue(result.gameID() > 0, "GameID should be > 0");

        // Confirm game is in DAO
        GameData storedGame = gameDAO.getGame(result.gameID());
        assertNotNull(storedGame, "Game should be stored with the returned ID");
        assertEquals("MyCoolGame", storedGame.gameName());
        assertNull(storedGame.whiteUsername());
        assertNull(storedGame.blackUsername());
    }

    @Test
    void testCreateGameInvalidToken() {
        CreateGameRequest request = new CreateGameRequest("bogus_token", "SomeGame");

        // Expect DataAccessException for invalid authToken
        assertThrows(DataAccessException.class, () -> gameService.createGame(request));
    }

    @Test
    void testCreateGameMissingGameName() throws DataAccessException {
        // Insert valid token
        AuthData validAuth = new AuthData("valid_token", "bob");
        authDAO.createAuth(validAuth);

        CreateGameRequest request = new CreateGameRequest("valid_token", "");

        assertThrows(DataAccessException.class, () -> gameService.createGame(request));
    }
}
