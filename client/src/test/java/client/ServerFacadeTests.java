package client;

import exception.ResponseException;
import model.AuthData;
import model.GameData;
import org.junit.jupiter.api.*;
import serverfacade.ServerFacade;
import service.results.ListGamesResult.GameInfo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ServerFacadeTests {
    private static server.Server server;
    private static ServerFacade facade;

    @BeforeAll
    static void setupServer() {
        server = new server.Server();
        int port = server.run(0);
        facade = new ServerFacade("http://localhost:" + port);
    }

    @AfterAll
    static void shutdown() {
        server.stop();
    }

    @BeforeEach
    void clearDatabase() throws ResponseException {
        facade.clear();
    }

    // REGISTER
    @Test void registerSuccess() throws Exception {
        AuthData auth = facade.register("user", "pw", "u@example.com");
        assertEquals("user", auth.username());
    }
    @Test void registerDuplicateFails() {
        assertDoesNotThrow(() -> facade.register("dup","pw","dup@e.com"));
        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.register("dup","pw","dup@e.com"));
        assertEquals("Error: already taken", ex.getMessage());
    }

    // LOGIN
    @Test void loginSuccess() throws Exception {
        facade.register("u","pw","u@e.com");
        AuthData auth = facade.login("u","pw");
        assertEquals("u", auth.username());
    }
    @Test void loginBadCredentialsFails() throws Exception {
        facade.register("u2","pw","u2@e.com");
        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.login("u2","wrong"));
        assertEquals("Error: unauthorized", ex.getMessage());
    }

    // LOGOUT
    @Test void logoutSuccess() throws Exception {
        AuthData auth = facade.register("x","pw","x@e.com");
        assertDoesNotThrow(() -> facade.logout(auth.authToken()));
    }
    @Test void logout_invalidToken_fails() {
        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.logout("badToken"));
        assertEquals("Error: unauthorized", ex.getMessage());
    }

    // CLEAR
    @Test void clearIdempotent() throws Exception {
        facade.clear(); // no-op on empty DB
        AuthData a = facade.register("a","pw","a@e.com");
        facade.clear();
        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.listGames(a.authToken()));
        assertEquals("Error: unauthorized", ex.getMessage());
    }
    @Test void clearEmptyNoError() {
        assertDoesNotThrow(() -> facade.clear());
    }

    // CREATE GAME
    @Test
    void createGameSuccess() throws Exception {
        AuthData auth = facade.register("host", "pw", "h@e.com");
        facade.createGame(auth.authToken(), "Game1");

        List<GameInfo> games = facade.listGames(auth.authToken());
        assertEquals(1, games.size());

        GameInfo info = games.get(0);
        assertTrue(info.gameID() > 0, "Expected a non-zero game ID");
    }
    @Test void createGameBadTokenFails() {
        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.createGame("bad","Game"));
        assertEquals("Error: unauthorized", ex.getMessage());
    }

    // LIST GAMES
    @Test void listGamesSuccess() throws Exception {
        AuthData auth = facade.register("l","pw","l@e.com");
        assertTrue(facade.listGames(auth.authToken()).isEmpty());
    }
    @Test void listGamesBadTokenFails() {
        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.listGames("badToken"));
        assertEquals("Error: unauthorized", ex.getMessage());
    }

    // JOIN GAME
    @Test void joinGameSuccess() throws Exception {
        AuthData host = facade.register("h2","pw","h2@e.com");
        GameData game = facade.createGame(host.authToken(), "G2");
        AuthData guest = facade.register("g2","pw","g2@e.com");
        facade.joinGame(guest.authToken(), game.gameID(), "WHITE");
        List<GameInfo> list = facade.listGames(host.authToken());
        assertEquals("g2", list.get(0).whiteUsername());
    }
    @Test void joinGameInvalidGameFails() throws Exception {
        AuthData u = facade.register("u3","pw","u3@e.com");
        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.joinGame(u.authToken(), 9999, "BLACK"));
        assertEquals("Error: bad request", ex.getMessage());
    }

}
