package service;

import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.UserDAO;
import model.AuthData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;
import requests.LoginRequest;
import requests.LogoutRequest;
import requests.RegisterRequest;
import results.LoginResult;
import results.LogoutResult;
import results.RegisterResult;

import java.util.UUID;

public class UserService {
    private final UserDAO userDAO;
    private final AuthDAO authDAO;

    public UserService(UserDAO userDAO, AuthDAO authDAO) {
        this.userDAO = userDAO;
        this.authDAO = authDAO;
    }

    /**
     * Create a new user and return an auth token
     */
    public RegisterResult register(RegisterRequest request) throws DataAccessException {
        // Basic validation
        if (request.username() == null || request.username().isBlank() ||
                request.password() == null || request.password().isBlank() ||
                request.email() == null || request.email().isBlank()) {
            throw new DataAccessException("Invalid request fields");
        }

        // Check if user already exists
        UserData existing = userDAO.getUser(request.username());
        if (existing != null) {
            throw new DataAccessException("User already taken: " + request.username());
        }

        // Hash password
        String hashedPassword = BCrypt.hashpw(request.password(), BCrypt.gensalt());

        // Create user with hashed password
        UserData newUser = new UserData(request.username(), hashedPassword, request.email());
        userDAO.createUser(newUser);

        // Generate & store auth token
        String token = UUID.randomUUID().toString();
        AuthData authData = new AuthData(token, request.username());
        authDAO.createAuth(authData);

        return new RegisterResult(request.username(), token);
    }

    /**
     * Logs in an existing user, creating and returning a new auth token.
     */
    public LoginResult login(LoginRequest request) throws DataAccessException {
        // Validate input
        if (request.username() == null || request.username().isBlank() ||
                request.password() == null || request.password().isBlank()) {
            throw new DataAccessException("Invalid login request fields (username/password)");
        }

        // Fetch user
        UserData user = userDAO.getUser(request.username());
        if (user == null) {
            throw new DataAccessException("User does not exist: " + request.username());
        }

        // Compare provided password with the stored (hashed) password
        if (!BCrypt.checkpw(request.password(), user.password())) {
            throw new DataAccessException("Incorrect password for user: " + request.username());
        }

        // Create new auth token
        String token = UUID.randomUUID().toString();
        AuthData authData = new AuthData(token, user.username());
        authDAO.createAuth(authData);

        return new LoginResult(user.username(), token);
    }

    /**
     * Logs out the user w/ this authToken
     */
    public LogoutResult logout(LogoutRequest request) throws DataAccessException {
        // Validate input
        if (request.authToken() == null || request.authToken().isBlank()) {
            throw new DataAccessException("Invalid logout request: authToken is missing");
        }

        // Look up token in authDAO
        AuthData authData = authDAO.getAuth(request.authToken());
        if (authData == null) {
            throw new DataAccessException("Invalid or non-existent auth token");
        }

        // Delete token, return success
        authDAO.deleteAuth(request.authToken());
        return new LogoutResult();
    }
}


