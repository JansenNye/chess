package dataaccess;

import chess.ChessGame;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example JUnit test class for GameDaoMySQL,
 * ensuring that referenced users exist before creating games.
 */
public class GameDAOMySQLTest {

    private GameDAO gameDao;
    private UserDAO userDao;

    @BeforeEach
    void setup() throws DataAccessException {
        userDao = new UserDAOMySQL();
        gameDao = new GameDAOMySQL();

        gameDao.clear();
        userDao.clear();

        userDao.createUser(new UserData("alice", "aliceHash", "alice@byu.edu"));
        userDao.createUser(new UserData("bob", "bobHash", "bob@byu.edu"));
    }

    @Test
    void testCreateGame_Positive() throws DataAccessException {
        ChessGame chess = new ChessGame();

        // Build GameData referencing existing users
        GameData game = new GameData(
                1234,
                "alice",
                "bob",
                "Friendly Match",
                chess   // ChessGame obj
        );

        assertDoesNotThrow(() -> gameDao.createGame(game));

        // Retrieve & verify
        GameData retrieved = gameDao.getGame(1234);
        assertNotNull(retrieved, "Game should exist after creation");
        assertEquals("alice", retrieved.whiteUsername());
        assertEquals("bob", retrieved.blackUsername());
        assertEquals("Friendly Match", retrieved.gameName());
        assertNotNull(retrieved.game(), "Should have a ChessGame object");
        assertEquals(chess.getTeamTurn(), retrieved.game().getTeamTurn(), "Team turn should match");
    }

    @Test
    void testCreateGame_Negative_DuplicateID() throws DataAccessException {
        // Insert game w/ ID=1111
        GameData g1 = new GameData(1111, "alice", "bob", "TestGame1", new ChessGame());
        gameDao.createGame(g1);

        // Attempt to insert another game w same ID
        GameData g2 = new GameData(1111, "alice", null, "TestGame2", new ChessGame());
        assertThrows(DataAccessException.class, () -> gameDao.createGame(g2));
    }

    @Test
    void testGetGame_Positive() throws DataAccessException {
        GameData g = new GameData(2222, "alice", null, "TestGame", new ChessGame());
        gameDao.createGame(g);

        GameData retrieved = gameDao.getGame(2222);
        assertNotNull(retrieved);
        assertEquals("alice", retrieved.whiteUsername());
        assertNull(retrieved.blackUsername());
    }

    @Test
    void testGetGame_Negative_NonExistent() throws DataAccessException {
        GameData retrieved = gameDao.getGame(9999);
        assertNull(retrieved, "Should return null for non-existent gameID");
    }

    @Test
    void testListGames_Positive() throws DataAccessException {
        // Insert multiple games
        gameDao.createGame(new GameData(3333, "alice", "bob", "GameOne", new ChessGame()));
        gameDao.createGame(new GameData(4444, "alice", null, "GameTwo", new ChessGame()));

        List<GameData> games = gameDao.listGames();
        assertEquals(2, games.size(), "Should have 2 games in the list");
    }

    @Test
    void testUpdateGame_Positive() throws DataAccessException {
        GameData original = new GameData(5555, "alice", null, "Original", new ChessGame());
        gameDao.createGame(original);

        // Retrieve & modify
        GameData retrieved = gameDao.getGame(5555);
        assertNotNull(retrieved);

        // Update black username to "bob" and gameName
        GameData updated = new GameData(
                retrieved.gameID(),
                retrieved.whiteUsername(),
                "bob", // now referencing existing user bob
                "UpdatedGame",
                retrieved.game() // same ChessGame object
        );  gameDao.updateGame(updated);

        // Retrieve again
        GameData afterUpdate = gameDao.getGame(5555);
        assertNotNull(afterUpdate);
        assertEquals("bob", afterUpdate.blackUsername());
        assertEquals("UpdatedGame", afterUpdate.gameName());
    }

    @Test
    void testClear_Positive() throws DataAccessException {
        gameDao.createGame(new GameData(6666, "alice", "bob", "ClearTest", new ChessGame()));
        gameDao.createGame(new GameData(7777, null, "bob", "ClearTest2", new ChessGame()));

        gameDao.clear();

        assertNull(gameDao.getGame(6666));
        assertNull(gameDao.getGame(7777));
    }
}