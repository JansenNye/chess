package service;
import dataaccess.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.requests.LoginRequest;
import service.requests.RegisterRequest;
import service.results.LoginResult;
import service.results.RegisterResult;
import model.UserData;
import model.AuthData;
import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {
    private UserDAO userDAO;
    private AuthDAO authDAO;
    private UserService userService;

    @BeforeEach
    void setup() {
        userDAO = new MemoryUserDAO();
        authDAO = new MemoryAuthDAO();
        userService = new UserService(userDAO, authDAO);
    }

    @Test
    void testRegister_Success() throws DataAccessException {
        // Start with an empty DB (MemoryUserDAO is empty initially)
        // Make a request
        RegisterRequest request = new RegisterRequest("alice", "secret", "alice@example.com");

        // Call service
        RegisterResult result = userService.register(request);

        // Check result
        assertNotNull(result);
        assertEquals("alice", result.username());
        assertNotNull(result.authToken());  // Should be a non-empty UUID

        // Check DAOs to confirm
        UserData userInDB = userDAO.getUser("alice");
        assertNotNull(userInDB);
        assertEquals("secret", userInDB.password());
        assertEquals("alice@example.com", userInDB.email());
        AuthData authInDB = authDAO.getAuth(result.authToken());
        assertNotNull(authInDB);
        assertEquals("alice", authInDB.username());
    }

    @Test
    void testRegister_UserAlreadyTaken() throws DataAccessException {
        // Pre-insert user
        userDAO.createUser(new UserData("bob", "123", "bob@example.com"));

        RegisterRequest request = new RegisterRequest("bob", "pass", "bob@domain.com");

        // Expect an exception if we try to re-register same user
        assertThrows(DataAccessException.class, () -> {
            userService.register(request);
        });
    }

    @Test
    void testRegister_InvalidInput() {
        // Try with blank username
        RegisterRequest badRequest = new RegisterRequest("", "secret", "email@x.com");
        assertThrows(DataAccessException.class, () -> {
            userService.register(badRequest);
        });
    }

    @Test
    void testLogin_Success() throws DataAccessException {
        // First, register user so they exist in the DAO
        RegisterRequest registerRequest = new RegisterRequest("alice", "secret", "alice@example.com");
        RegisterResult registerResult = userService.register(registerRequest);
        assertNotNull(registerResult.authToken());  // user is created, so we have a token

        // Attempt to log in w/ correct credentials
        LoginRequest loginRequest = new LoginRequest("alice", "secret");
        LoginResult loginResult = userService.login(loginRequest);

        // Verify login result
        assertNotNull(loginResult);
        assertEquals("alice", loginResult.username());
        assertNotNull(loginResult.authToken());

        // Check that new auth token was really created in the DAO
        AuthData authData = authDAO.getAuth(loginResult.authToken());
        assertNotNull(authData);
        assertEquals("alice", authData.username());
    }

    @Test
    void testLogin_NoSuchUser() throws DataAccessException {
        // Attempt to log in with nonexistent user
        LoginRequest loginRequest = new LoginRequest("bob", "secret");
        assertThrows(DataAccessException.class, () -> {
            userService.login(loginRequest);
        });
    }

    @Test
    void testLogin_BadPassword() throws DataAccessException {
        // Register a user
        RegisterRequest registerRequest = new RegisterRequest("charlie", "password", "charlie@example.com");
        userService.register(registerRequest);

        // Try to log in with wrong password
        LoginRequest badPasswordReq = new LoginRequest("charlie", "wrongPassword");
        assertThrows(DataAccessException.class, () -> {
            userService.login(badPasswordReq);
        });
    }

    @Test
    void testLogin_MissingFields() {
        // Missing username
        LoginRequest badRequest1 = new LoginRequest(null, "secret");
        // Missing password
        LoginRequest badRequest2 = new LoginRequest("dave", "");

        assertThrows(DataAccessException.class, () -> {
            userService.login(badRequest1);
        });
        assertThrows(DataAccessException.class, () -> {
            userService.login(badRequest2);
        });
    }
}

