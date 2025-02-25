package service;

import dataaccess.*;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ClearServiceTest {

    private UserDAO userDAO;
    private GameDAO gameDAO;
    private AuthDAO authDAO;
    private ClearService clearService;

    @BeforeEach
    void setup() {
        userDAO = new MemoryUserDAO();
        gameDAO = new MemoryGameDAO();
        authDAO = new MemoryAuthDAO();
        clearService = new ClearService(userDAO, gameDAO, authDAO);
    }

    @Test
    void testClearEmptiesAllData() throws DataAccessException {
        // Insert test data
        userDAO.createUser(new UserData("alice", "secret", "alice@example.com"));
        gameDAO.createGame(new GameData(1, "alice", null, "TestGame", null));
        authDAO.createAuth(new AuthData("tok123", "alice"));

        // Clear
        clearService.clear();

        // Check that clear worked
        assertNull(userDAO.getUser("alice"));
        assertNull(gameDAO.getGame(1));
        assertNull(authDAO.getAuth("tok123"));
    }

    @Test
    void testClear_ThrowsException() {
        // 1) Make a special userDAO that always fails on clear()
        UserDAO throwingUserDAO = new MemoryUserDAO();
        ClearService badClearService = new ClearService(throwingUserDAO, gameDAO, authDAO);

        // 2) Expect an exception
        assertThrows(DataAccessException.class, () -> {
            badClearService.clear();
        });
    }
}
