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
        // Instantiate both DAOs
        userDao = new UserDAOMySQL();
        gameDao = new GameDAOMySQL();

        // Clear tables before each test
        // (Order can matter if you have foreign keys, but if you have ON DELETE CASCADE, it's less critical.)
        gameDao.clear();
        userDao.clear();

        // Insert some users so we can reference them in games
        // (Necessary if white_username or black_username references users(username))
        userDao.createUser(new UserData("alice", "aliceHash", "alice@byu.edu"));
        userDao.createUser(new UserData("bob", "bobHash", "bob@byu.edu"));
    }

    @Test
    void testCreateGame_Positive() throws DataAccessException {
        // Create a ChessGame object
        ChessGame chess = new ChessGame();

        // Build GameData referencing existing users
        GameData game = new GameData(
                1234,             // game_id
                "alice",          // white_username
                "bob",            // black_username
                "Friendly Match", // game_name
                chess             // ChessGame object
        );

        // This should succeed now that "alice" and "bob" exist in the users table
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
        // Insert a game with ID=1111
        GameData g1 = new GameData(1111, "alice", "bob", "TestGame1", new ChessGame());
        gameDao.createGame(g1);

        // Attempt to insert another game with the same ID
        GameData g2 = new GameData(1111, "alice", null, "TestGame2", new ChessGame());
        // Expect a DataAccessException if game_id is a PRIMARY KEY
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

        // Let's update the blackUsername to "bob" and gameName
        GameData updated = new GameData(
                retrieved.gameID(),
                retrieved.whiteUsername(),
                "bob", // now referencing existing user bob
                "UpdatedGame",
                retrieved.game() // same ChessGame object
        );
        gameDao.updateGame(updated);

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