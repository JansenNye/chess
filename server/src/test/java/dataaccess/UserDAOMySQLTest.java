package dataaccess;

import model.UserData;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class UserDAOMySQLTest {

    private UserDAO userDao;

    @BeforeEach
    void setup() throws DataAccessException {
        userDao = new UserDAOMySQL();
        // Clear table before each test
        userDao.clear();
    }

    @Test
    void testCreateUser_Positive() throws DataAccessException {
        UserData user = new UserData("alice", "hashedPass123", "alice@byu.edu");
        assertDoesNotThrow(() -> userDao.createUser(user));

        // retrieve and check
        UserData retrieved = userDao.getUser("alice");
        assertNotNull(retrieved, "User should exist after creation");
        assertEquals("alice", retrieved.username());
        assertEquals("hashedPass123", retrieved.password());
        assertEquals("alice@byu.edu", retrieved.email());
    }

    @Test
    void testCreateUser_Negative_DuplicateUsername() throws DataAccessException {
        // Insert user once
        UserData user1 = new UserData("bob", "someHash", "bob@byu.edu");
        userDao.createUser(user1);

        // Insert a second user with the same username
        UserData user2 = new UserData("bob", "someOtherHash", "bob2@byu.edu");

        // Expect a DataAccessException or however your code signals duplicates
        assertThrows(DataAccessException.class, () -> userDao.createUser(user2));
    }

    @Test
    void testGetUser_Positive() throws DataAccessException {
        // Create and retrieve
        UserData user = new UserData("charlie", "charlieHash", "charlie@byu.edu");
        userDao.createUser(user);

        UserData retrieved = userDao.getUser("charlie");
        assertNotNull(retrieved);
        assertEquals("charlie", retrieved.username());
    }

    @Test
    void testGetUser_Negative_NonExistent() throws DataAccessException {
        // No users created, so retrieval should return null
        UserData retrieved = userDao.getUser("nonexistent");
        assertNull(retrieved, "Should return null for non-existent user");
    }

    @Test
    void testClear_Positive() throws DataAccessException {
        // Insert multiple users
        userDao.createUser(new UserData("dave", "hash1", "dave@byu.edu"));
        userDao.createUser(new UserData("eve", "hash2", "eve@byu.edu"));

        // Clear
        userDao.clear();

        // Both should be gone
        assertNull(userDao.getUser("dave"));
        assertNull(userDao.getUser("eve"));
    }
}