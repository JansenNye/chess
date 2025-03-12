package dataaccess;

import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserDAOMySQLTest {

    private UserDAO userDao;

    @BeforeEach
    void setup() throws DataAccessException {
        GameDAO gameDao = new GameDAOMySQL();
        AuthDAO authDao = new AuthDAOMySQL();
        userDao = new UserDAOMySQL();

        // child to parent order
        gameDao.clear();
        authDao.clear();
        userDao.clear();
    }

    @Test
    void testCreateUser_Positive() throws DataAccessException {
        UserData user = new UserData("alice", "hashedblah", "alice@byu.edu");
        assertDoesNotThrow(() -> userDao.createUser(user));

        UserData retrieved = userDao.getUser("alice");
        assertNotNull(retrieved, "User should exist after creation");
        assertEquals("alice", retrieved.username());
        assertEquals("hashedblah", retrieved.password());
        assertEquals("alice@byu.edu", retrieved.email());
    }

    @Test
    void testCreateUser_Negative_DuplicateUsername() throws DataAccessException {
        UserData user1 = new UserData("bob", "blahblah1", "bob@byu.edu");
        userDao.createUser(user1);

        UserData user2 = new UserData("bob", "blahblah2", "bob2@byu.edu");
        assertThrows(DataAccessException.class, () -> userDao.createUser(user2));
    }

    @Test
    void testGetUserPositive() throws DataAccessException {
        UserData user = new UserData("charlie", "charlieHash", "charlie@byu.edu");
        userDao.createUser(user);

        UserData retrieved = userDao.getUser("charlie");
        assertNotNull(retrieved);
        assertEquals("charlie", retrieved.username());
    }

    @Test
    void testGetUserNegativeNonExistent() throws DataAccessException {
        // No users created, retrieval should return null
        UserData retrieved = userDao.getUser("nonexistent");
        assertNull(retrieved, "Should return null for non-existent user");
    }

    @Test
    void testClearPositive() throws DataAccessException {
        userDao.createUser(new UserData("dave", "hash1", "dave@byu.edu"));
        userDao.createUser(new UserData("evie", "hash2", "evie@byu.edu"));

        userDao.clear();
        assertNull(userDao.getUser("dave"));
        assertNull(userDao.getUser("evie"));
    }
}