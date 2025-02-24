package service;

import dataaccess.*;
import model.AuthData;
import model.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.requests.JoinGameRequest;
import service.requests.ListGamesRequest;
import service.results.JoinGameResult;
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

    @Test
    void testJoinGame_WhiteSuccess() throws DataAccessException {
        // Create valid auth
        AuthData auth = new AuthData("token123", "alice");
        authDAO.createAuth(auth);

        // Create a game with no players
        GameData newGame = new GameData(1001, null, null, "TestGame", null);
        gameDAO.createGame(newGame);

        // Make request
        JoinGameRequest request = new JoinGameRequest("token123", "WHITE", 1001);

        // Call joinGame
        JoinGameResult result = gameService.joinGame(request);
        assertNotNull(result);

        // Verify game was updated
        GameData updatedGame = gameDAO.getGame(1001);
        assertEquals("alice", updatedGame.whiteUsername());
        assertNull(updatedGame.blackUsername());
    }

    @Test
    void testJoinGame_BlackSuccess() throws DataAccessException {
        // Similar but fill blackUsername
        authDAO.createAuth(new AuthData("token456", "bob"));
        gameDAO.createGame(new GameData(2002, null, null, "AnotherGame", null));

        JoinGameRequest request = new JoinGameRequest("token456", "BLACK", 2002);
        gameService.joinGame(request);

        GameData updated = gameDAO.getGame(2002);
        assertNull(updated.whiteUsername());
        assertEquals("bob", updated.blackUsername());
    }

    @Test
    void testJoinGame_InvalidToken() throws DataAccessException {
        // No tokens in DAO => "invalid"
        gameDAO.createGame(new GameData(3003, null, null, "GameNoAuth", null));

        JoinGameRequest req = new JoinGameRequest("bogus", "WHITE", 3003);
        assertThrows(DataAccessException.class, () -> {
            gameService.joinGame(req);
        });
    }

    @Test
    void testJoinGame_GameNotFound() throws DataAccessException {
        // We do have a valid token
        authDAO.createAuth(new AuthData("token789", "charlie"));

        // But no game with ID=9999
        JoinGameRequest req = new JoinGameRequest("token789", "WHITE", 9999);
        assertThrows(DataAccessException.class, () -> {
            gameService.joinGame(req);
        });
    }

    @Test
    void testJoinGame_ColorAlreadyTaken() throws DataAccessException {
        // token - "dave"
        authDAO.createAuth(new AuthData("tokenD", "dave"));
        // Pre-insert a game that already has "eve" as WHITE
        gameDAO.createGame(new GameData(4004, "eve", null, "FullWhiteSlot", null));

        // Now dave tries to join as WHITE - not available
        JoinGameRequest req = new JoinGameRequest("tokenD", "WHITE", 4004);
        assertThrows(DataAccessException.class, () -> {
            gameService.joinGame(req);
        });
    }

    @Test
    void testJoinGame_AlreadyJoined() throws DataAccessException {
        // Suppose "eve" is already in the black slot
        authDAO.createAuth(new AuthData("tokenE", "eve"));
        gameDAO.createGame(new GameData(5005, null, "eve", "BlackSlotFull", null));

        // Now "eve" tries to join again as BLACK => we can allow or disallow
        JoinGameRequest req = new JoinGameRequest("tokenE", "BLACK", 5005);

        // The example code just sets the black slot to "eve" again. No error.
        // So let's just check it doesn't throw:
        assertDoesNotThrow(() -> gameService.joinGame(req));

        // Check that blackUsername remains "eve"
        GameData updated = gameDAO.getGame(5005);
        assertEquals("eve", updated.blackUsername());
    }

}
