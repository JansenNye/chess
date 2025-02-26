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
}

