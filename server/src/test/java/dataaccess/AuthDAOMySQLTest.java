package dataaccess;

import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthDAOMySQLTest {

    private AuthDAO authDao;
    private UserDAO userDao;
    private GameDAO gameDao;

    @BeforeEach
    void setup() throws DataAccessException {
        gameDao = new GameDAOMySQL();
        authDao = new AuthDAOMySQL();
        userDao = new UserDAOMySQL();

        // child of users
        gameDao.clear();
        // child of users
        authDao.clear();
        // users
        userDao.clear();

        // Insert user for referencing in auth
        userDao.createUser(new UserData("alice", "hash", "alice@byu.edu"));
    }

    @Test
    void testCreateAuth_Positive() throws DataAccessException {
        AuthData auth = new AuthData("token123", "alice");
        assertDoesNotThrow(() -> authDao.createAuth(auth));

        AuthData retrieved = authDao.getAuth("token123");
        assertNotNull(retrieved);
        assertEquals("alice", retrieved.username());
    }

    @Test
    void testCreateAuth_Negative_NonExistentUser() {
        AuthData badAuth = new AuthData("badToken", "jansen");
        assertThrows(DataAccessException.class, () -> authDao.createAuth(badAuth));
    }

    @Test
    void testGetAuth_Positive() throws DataAccessException {
        authDao.createAuth(new AuthData("tokentokentoken", "alexa"));
        AuthData retrieved = authDao.getAuth("tokentokentoken");
        assertNotNull(retrieved);
        assertEquals("alexa", retrieved.username());
    }

    @Test
    void testGetAuth_Negative_NonExistentToken() throws DataAccessException {
        AuthData retrieved = authDao.getAuth("noSuchToken");
        assertNull(retrieved, "Should return null for non-existent token");
    }

    @Test
    void testDeleteAuth_Positive() throws DataAccessException {
        authDao.createAuth(new AuthData("token111", "alexa"));
        authDao.deleteAuth("token111");
        assertNull(authDao.getAuth("token111"), "Token should be deleted");
    }

    @Test
    void testClear_Positive() throws DataAccessException {
        authDao.createAuth(new AuthData("t1", "alice"));
        authDao.createAuth(new AuthData("t2", "alice"));

        authDao.clear();

        assertNull(authDao.getAuth("t1"));
        assertNull(authDao.getAuth("t2"));
    }
}
