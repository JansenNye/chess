package dataaccess;

import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserDAOMySQLTest {

    private UserDAO userDao;
    private GameDAO gameDao;
    private AuthDAO authDao;

    @BeforeEach
    void setup() throws DataAccessException {
        // Instantiate all DAOs
        gameDao = new GameDAOMySQL();  // if 'games' references 'users'
        authDao = new AuthDAOMySQL();  // if 'auth' references 'users'
        userDao = new UserDAOMySQL();

        // Clear in child-first, parent-last order:
        // 1) games
        gameDao.clear();
        // 2) auth
        authDao.clear();
        // 3) users
        userDao.clear();
    }

    @Test
    void testCreateUser_Positive() throws DataAccessException {
        UserData user = new UserData("alice", "hashedPass123", "alice@byu.edu");
        assertDoesNotThrow(() -> userDao.createUser(user));

        UserData retrieved = userDao.getUser("alice");
        assertNotNull(retrieved, "User should exist after creation");
        assertEquals("alice", retrieved.username());
        assertEquals("hashedPass123", retrieved.password());
        assertEquals("alice@byu.edu", retrieved.email());
    }

    @Test
    void testCreateUser_Negative_DuplicateUsername() throws DataAccessException {
        UserData user1 = new UserData("bob", "someHash", "bob@byu.edu");
        userDao.createUser(user1);

        UserData user2 = new UserData("bob", "someOtherHash", "bob2@byu.edu");
        assertThrows(DataAccessException.class, () -> userDao.createUser(user2));
    }

    @Test
    void testGetUser_Positive() throws DataAccessException {
        UserData user = new UserData("charlie", "charlieHash", "charlie@byu.edu");
        userDao.createUser(user);

        UserData retrieved = userDao.getUser("charlie");
        assertNotNull(retrieved);
        assertEquals("charlie", retrieved.username());
    }

    @Test
    void testGetUser_Negative_NonExistent() throws DataAccessException {
        UserData retrieved = userDao.getUser("nonexistent");
        assertNull(retrieved, "Should return null for non-existent user");
    }

    @Test
    void testClear_Positive() throws DataAccessException {
        userDao.createUser(new UserData("dave", "hash1", "dave@byu.edu"));
        userDao.createUser(new UserData("eve", "hash2", "eve@byu.edu"));

        userDao.clear();
        assertNull(userDao.getUser("dave"));
        assertNull(userDao.getUser("eve"));
    }
}