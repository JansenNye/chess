package dataaccess;

import model.AuthData;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class AuthDAOMySQLTest {

    private AuthDAO authDao;
    private UserDAO userDao;

    @BeforeEach
    void setup() throws DataAccessException {
        authDao = new AuthDAOMySQL();
        userDao = new UserDAOMySQL();

        authDao.clear();
        userDao.clear();

        userDao.createUser(new model.UserData("alice", "hash", "alice@byu.edu"));
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
        AuthData badAuth = new AuthData("badToken", "bob"); // bob doesn't exist
        assertThrows(DataAccessException.class, () -> authDao.createAuth(badAuth));
    }

    @Test
    void testGetAuth_Positive() throws DataAccessException {
        authDao.createAuth(new AuthData("token456", "alice"));
        AuthData retrieved = authDao.getAuth("token456");
        assertNotNull(retrieved);
        assertEquals("alice", retrieved.username());
    }

    @Test
    void testGetAuth_Negative_NonExistentToken() throws DataAccessException {
        AuthData retrieved = authDao.getAuth("noSuchToken");
        assertNull(retrieved, "Should return null for non-existent token");
    }

    @Test
    void testDeleteAuth_Positive() throws DataAccessException {
        authDao.createAuth(new AuthData("token789", "alice"));
        authDao.deleteAuth("token789");
        assertNull(authDao.getAuth("token789"), "Token should be deleted");
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
