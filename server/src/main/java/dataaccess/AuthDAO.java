package dataaccess;

import model.AuthData;

public interface AuthDAO {

    /**
     * CLEAR endpoint - remove all auth tokens
     */
    void clear() throws DataAccessException;

    /**
     * Create a new auth token record
     */
    void createAuth(AuthData auth) throws DataAccessException;

    /**
     * Retrieve an auth record by token string
     */
    AuthData getAuth(String authToken) throws DataAccessException;

    /**
     * Delete an auth record (logout)
     */
    void deleteAuth(String authToken) throws DataAccessException;
}

