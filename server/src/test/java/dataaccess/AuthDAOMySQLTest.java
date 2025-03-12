package dataaccess;

import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthDAOMySQLTest {

    private AuthDAO authDao;

    @BeforeEach
    void setup() throws DataAccessException {
        GameDAO gameDao = new GameDAOMySQL();
        authDao = new AuthDAOMySQL();
        UserDAO userDao = new UserDAOMySQL();

        gameDao.clear();  // child
        authDao.clear();  // child
        userDao.clear();  // parent

        userDao.createUser(new UserData("alice", "blah", "alice@byu.edu"));
    }

    @Test
    void testCreateAuthPositive() throws DataAccessException {
        AuthData auth = new AuthData("tokenblah", "alice");
        assertDoesNotThrow(() -> authDao.createAuth(auth));

        // Retrieve & verify
        AuthData retrieved = authDao.getAuth("tokenblah");
        assertNotNull(retrieved);
        assertEquals("alice", retrieved.username());
    }

    @Test
    void testCreateAuthNegativeNonExistentUser() {
        AuthData badAuth = new AuthData("badToken", "jansen");
        assertThrows(DataAccessException.class, () -> authDao.createAuth(badAuth));
    }

    @Test
    void testGetAuthPositive() throws DataAccessException {
        authDao.createAuth(new AuthData("token456", "alice"));
        AuthData retrieved = authDao.getAuth("token456");
        assertNotNull(retrieved);
        assertEquals("alice", retrieved.username());
    }

    @Test
    void testGetAuthNegativeNonExistentToken() throws DataAccessException {
        AuthData retrieved = authDao.getAuth("noSuchToken");
        assertNull(retrieved, "Should return null for non-existent token");
    }

    @Test
    void testDeleteAuthPositive() throws DataAccessException {
        authDao.createAuth(new AuthData("token789", "alice"));
        authDao.deleteAuth("token789");
        assertNull(authDao.getAuth("token789"), "Token should be deleted");
    }

    @Test
    void testClearPositive() throws DataAccessException {
        authDao.createAuth(new AuthData("t1", "alice"));
        authDao.createAuth(new AuthData("t2", "alice"));

        authDao.clear();

        assertNull(authDao.getAuth("t1"));
        assertNull(authDao.getAuth("t2"));
    }
}
