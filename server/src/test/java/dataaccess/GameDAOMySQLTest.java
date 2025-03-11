package dataaccess;

import chess.ChessGame;
import model.GameData;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

public class GameDAOMySQLTest {

    private GameDAO gameDao;

    @BeforeEach
    void setup() throws DataAccessException {
        gameDao = new GameDAOMySQL();
        // Clear before each test
        gameDao.clear();
    }

    @Test
    void testCreateGame_Positive() throws DataAccessException {
        // Create a ChessGame object
        ChessGame chess = new ChessGame();

        // Build GameData
        GameData game = new GameData(
                1234,
                "alice",
                "bob",
                "Friendly Match",
                chess
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
        // Some DB setups might allow duplicate IDs if you didn't set them as PRIMARY KEY.
        // But let's assume game_id is primary key. Insert the same ID twice -> fail
        GameData g1 = new GameData(1111, null, null, "TestGame1", new ChessGame());
        gameDao.createGame(g1);

        GameData g2 = new GameData(1111, "alice", null, "TestGame2", new ChessGame());
        assertThrows(DataAccessException.class, () -> gameDao.createGame(g2));
    }

    @Test
    void testGetGame_Positive() throws DataAccessException {
        GameData g = new GameData(2222, "whiteUser", null, "TestGame", new ChessGame());
        gameDao.createGame(g);

        GameData retrieved = gameDao.getGame(2222);
        assertNotNull(retrieved);
        assertEquals("whiteUser", retrieved.whiteUsername());
    }

    @Test
    void testGetGame_Negative_NonExistent() throws DataAccessException {
        GameData retrieved = gameDao.getGame(9999);
        assertNull(retrieved, "Should return null for non-existent gameID");
    }

    @Test
    void testListGames_Positive() throws DataAccessException {
        // Insert multiple games
        gameDao.createGame(new GameData(3333, "w1", "b1", "GameOne", new ChessGame()));
        gameDao.createGame(new GameData(4444, "w2", "b2", "GameTwo", new ChessGame()));

        List<GameData> games = gameDao.listGames();
        assertEquals(2, games.size(), "Should have 2 games in the list");
    }

    @Test
    void testUpdateGame_Positive() throws DataAccessException {
        GameData original = new GameData(5555, null, null, "Original", new ChessGame());
        gameDao.createGame(original);

        // Retrieve & modify
        GameData retrieved = gameDao.getGame(5555);
        assertNotNull(retrieved);
        ChessGame updatedChess = retrieved.game();
        updatedChess.setTeamTurn(ChessGame.TeamColor.BLACK); // example modification

        // Set whiteUsername
        GameData updated = new GameData(
                retrieved.gameID(),
                "whiteUser",
                retrieved.blackUsername(),
                "UpdatedGame",
                updatedChess
        );
        gameDao.updateGame(updated);

        // Retrieve again
        GameData afterUpdate = gameDao.getGame(5555);
        assertNotNull(afterUpdate);
        assertEquals("whiteUser", afterUpdate.whiteUsername());
        assertEquals("UpdatedGame", afterUpdate.gameName());
        assertEquals(ChessGame.TeamColor.BLACK, afterUpdate.game().getTeamTurn());
    }

    @Test
    void testClear_Positive() throws DataAccessException {
        gameDao.createGame(new GameData(6666, "alice", "bob", "ClearTest", new ChessGame()));
        gameDao.createGame(new GameData(7777, null, null, "ClearTest2", new ChessGame()));

        gameDao.clear();

        assertNull(gameDao.getGame(6666));
        assertNull(gameDao.getGame(7777));
    }
}