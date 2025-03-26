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
    @Test void register_success() throws Exception {
        AuthData auth = facade.register("user", "pw", "u@example.com");
        assertEquals("user", auth.username());
    }
    @Test void register_duplicate_fails() {
        assertDoesNotThrow(() -> facade.register("dup","pw","dup@e.com"));
        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.register("dup","pw","dup@e.com"));
        assertEquals("Error: already taken", ex.getMessage());
    }

    // LOGIN
    @Test void login_success() throws Exception {
        facade.register("u","pw","u@e.com");
        AuthData auth = facade.login("u","pw");
        assertEquals("u", auth.username());
    }
    @Test void login_badCredentials_fails() throws Exception {
        facade.register("u2","pw","u2@e.com");
        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.login("u2","wrong"));
        assertEquals("Error: unauthorized", ex.getMessage());
    }

    // LOGOUT
    @Test void logout_success() throws Exception {
        AuthData auth = facade.register("x","pw","x@e.com");
        assertDoesNotThrow(() -> facade.logout(auth.authToken()));
    }
    @Test void logout_invalidToken_fails() {
        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.logout("badToken"));
        assertEquals("Error: unauthorized", ex.getMessage());
    }

    // CLEAR
    @Test void clear_idempotent() throws Exception {
        facade.clear();
        AuthData a = facade.register("a","pw","a@e.com");
        facade.clear();
        assertTrue(facade.listGames(a.authToken()).isEmpty());
    }
    @Test void clear_empty_noError() {
        assertDoesNotThrow(() -> facade.clear());
    }

    // CREATE GAME
    @Test
    void createGame_success() throws Exception {
        AuthData auth = facade.register("host", "pw", "h@e.com");
        facade.createGame(auth.authToken(), "Game1");

        List<GameInfo> games = facade.listGames(auth.authToken());
        assertEquals(1, games.size());

        GameInfo info = games.get(0);
        assertTrue(info.gameID() > 0, "Expected a non-zero game ID");
    }
    @Test void createGame_badToken_fails() {
        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.createGame("bad","Game"));
        assertEquals("Error: unauthorized", ex.getMessage());
    }

    // LIST GAMES
    @Test void listGames_success() throws Exception {
        AuthData auth = facade.register("l","pw","l@e.com");
        assertTrue(facade.listGames(auth.authToken()).isEmpty());
    }
    @Test void listGames_badToken_fails() {
        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.listGames("badToken"));
        assertEquals("Error: unauthorized", ex.getMessage());
    }

    // JOIN GAME
    @Test void joinGame_success() throws Exception {
        AuthData host = facade.register("h2","pw","h2@e.com");
        GameData game = facade.createGame(host.authToken(), "G2");
        AuthData guest = facade.register("g2","pw","g2@e.com");
        facade.joinGame(guest.authToken(), game.gameID(), "WHITE");
        List<GameInfo> list = facade.listGames(host.authToken());
        assertEquals("g2", list.get(0).whiteUsername());
    }
    @Test void joinGame_invalidGame_fails() throws Exception {
        AuthData u = facade.register("u3","pw","u3@e.com");
        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.joinGame(u.authToken(), 9999, "BLACK"));
        assertEquals("Error: bad request", ex.getMessage());
    }

}
