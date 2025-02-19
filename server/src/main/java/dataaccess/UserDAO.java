package dataaccess;
import model.UserData;

public interface UserDAO {
    /**
     * CLEAR endpoint - remove all user data
     */
    void clear() throws DataAccessException;

    /**
     *
     * Create/insert a new user
     */
    void createUser(UserData user) throws DataAccessException;

    /**
     * Retrieve a user by username
     */
    UserData getUser(String username) throws DataAccessException;

    /**
     * Update user’s data
     */
    void updateUser(UserData user) throws DataAccessException;

    /**
     * Removes a user (optional, if your design needs it).
     */
    void deleteUser(String username) throws DataAccessException;
}

