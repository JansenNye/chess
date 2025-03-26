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
    private static ServerFacade facade;

    @BeforeAll
    public static void init() {
        var server = new server.Server();
        int port = server.run(0);
        facade = new ServerFacade("http://localhost:" + port);
    }

    @BeforeEach
    void clear() throws ResponseException {
        facade.clear();
    }

    @Test
    void registerLoginLogout_success() throws ResponseException {
        AuthData reg = facade.register("user1", "pw", "user1@example.com");
        assertNotNull(reg.authToken());
        assertEquals("user1", reg.username());

        AuthData login = facade.login("user1", "pw");
        assertEquals("user1", login.username());
        assertNotEquals(reg.authToken(), login.authToken());

        facade.logout(login.authToken());
    }

    @Test
    void registerDuplicate_throws() {
        assertDoesNotThrow(() -> facade.register("dup", "pw", "dup@example.com"));
        ResponseException ex = assertThrows(ResponseException.class,
                () -> facade.register("dup", "pw2", "dup2@example.com"));
        assertEquals(500, ex.getStatusCode());
    }

    @Test
    void listGames_emptyInitially() throws ResponseException {
        AuthData user = facade.register("a", "pw", "a@example.com");
        List<GameInfo> games = facade.listGames(user.authToken());
        assertTrue(games.isEmpty());
    }

    @Test
    void createListJoinGame() throws ResponseException {
        AuthData host = facade.register("host", "pw", "host@example.com");
        GameData created = facade.createGame(host.authToken(), "MyGame");

        List<GameInfo> games = facade.listGames(host.authToken());
        assertEquals(1, games.size());
        assertEquals(created.gameID(), games.getFirst().gameID());

        AuthData guest = facade.register("guest", "pw", "guest@example.com");
        facade.joinGame(guest.authToken(), created.gameID(), "WHITE");
    }
}