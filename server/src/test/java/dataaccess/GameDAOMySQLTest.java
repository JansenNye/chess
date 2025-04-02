package dataaccess;

import chess.ChessGame;
import model.GameData;
import model.GameStatus;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameDAOMySQLTest {

    private GameDAO gameDao;

    @BeforeEach
    void setup() throws DataAccessException {
        gameDao = new GameDAOMySQL();
        AuthDAO authDao = new AuthDAOMySQL();
        UserDAO userDao = new UserDAOMySQL();

        // child-first, parent-last
        gameDao.clear();
        authDao.clear();
        userDao.clear();

        userDao.createUser(new UserData("alice", "aliceHash", "alice@byu.edu"));
        userDao.createUser(new UserData("bob", "bobHash", "bob@byu.edu"));
    }

    @Test
    void testCreateGamePositive() throws DataAccessException {
        ChessGame chess = new ChessGame();
        GameData game = new GameData(
                1234,
                "alice",
                "bob",
                "Friendly Match",
                chess,
                GameStatus.ACTIVE
        );

        assertDoesNotThrow(() -> gameDao.createGame(game));

        GameData retrieved = gameDao.getGame(1234);
        assertNotNull(retrieved, "Game should exist after creation");
        assertEquals("alice", retrieved.whiteUsername());
        assertEquals("bob", retrieved.blackUsername());
        assertEquals("Friendly Match", retrieved.gameName());
        assertNotNull(retrieved.game());
        assertEquals(chess.getTeamTurn(), retrieved.game().getTeamTurn());
    }

    @Test
    void testCreateGameNegativeDuplicateID() throws DataAccessException {
        GameData g1 = new GameData(1111, "alice", "bob", "TestGame1", new ChessGame(), GameStatus.ACTIVE);
        gameDao.createGame(g1);

        // Attempt to create another game with same ID
        GameData g2 = new GameData(1111, "alice", null, "TestGame2", new ChessGame(), GameStatus.ACTIVE);
        assertThrows(DataAccessException.class, () -> gameDao.createGame(g2));
    }

    @Test
    void testGetGamePositive() throws DataAccessException {
        GameData g = new GameData(2222, "alice", null, "TestGame", new ChessGame(),  GameStatus.ACTIVE);
        gameDao.createGame(g);

        GameData retrieved = gameDao.getGame(2222);
        assertNotNull(retrieved);
        assertEquals("alice", retrieved.whiteUsername());
        assertNull(retrieved.blackUsername());
    }

    @Test
    void testGetGameNegativeNonExistent() throws DataAccessException {
        GameData retrieved = gameDao.getGame(9999);
        assertNull(retrieved, "Should return null for non-existent gameID");
    }

    @Test
    void testListGamesPositive() throws DataAccessException {
        gameDao.createGame(new GameData(3333, "alice", "bob", "GameOne", new ChessGame(), GameStatus.ACTIVE));
        gameDao.createGame(new GameData(4444, "alice", null, "GameTwo", new ChessGame(), GameStatus.ACTIVE));

        List<GameData> games = gameDao.listGames();
        assertEquals(2, games.size(), "Should have 2 games in the list");
    }

    @Test
    void testUpdateGamePositive() throws DataAccessException {
        GameData original = new GameData(5555, "alice", null, "Original", new ChessGame(), GameStatus.ACTIVE);
        gameDao.createGame(original);

        // Retrieve & modify
        GameData retrieved = gameDao.getGame(5555);
        assertNotNull(retrieved);
        ChessGame updatedChess = retrieved.game();
        updatedChess.setTeamTurn(chess.ChessGame.TeamColor.BLACK);

        // Set blackUsername
        GameData updated = new GameData(
                retrieved.gameID(),
                retrieved.whiteUsername(),
                "bob",  // referencing an existing user
                "UpdatedGame",
                updatedChess,
                GameStatus.ACTIVE
        );
        gameDao.updateGame(updated);

        GameData afterUpdate = gameDao.getGame(5555);
        assertNotNull(afterUpdate);
        assertEquals("bob", afterUpdate.blackUsername());
        assertEquals("UpdatedGame", afterUpdate.gameName());
        assertEquals(chess.ChessGame.TeamColor.BLACK, afterUpdate.game().getTeamTurn());
    }

    @Test
    void testClearPositive() throws DataAccessException {
        gameDao.createGame(new GameData(6666, "alice", "bob", "ClearTest", new ChessGame(), GameStatus.ACTIVE));
        gameDao.createGame(new GameData(7777, "alice", null, "ClearTest2", new ChessGame(), GameStatus.ACTIVE));

        gameDao.clear();

        assertNull(gameDao.getGame(6666));
        assertNull(gameDao.getGame(7777));
    }
}