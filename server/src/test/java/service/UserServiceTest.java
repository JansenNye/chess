package service;

import dataaccess.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import requests.LoginRequest;
import requests.LogoutRequest;
import requests.RegisterRequest;
import results.LoginResult;
import results.LogoutResult;
import results.RegisterResult;
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
    void testRegisterSuccess() throws DataAccessException {
        // Start with an empty DB, make a request
        RegisterRequest request = new RegisterRequest("aliceusername", "alicepassword", "alicetest@gmail.com");

        // Call service
        RegisterResult result = userService.register(request);

        // Check result
        assertNotNull(result);
        assertEquals("aliceusername", result.username());
        assertNotNull(result.authToken());  // Should be a non-empty UUID

        // Check DAOs to confirm
        UserData userInDB = userDAO.getUser("aliceusername");
        assertNotNull(userInDB);

        // UPDATED/FIXED - hashing
        assertTrue(BCrypt.checkpw("alicepassword", userInDB.password()),
                "Database-stored password should be a valid bcrypt hash matching 'alicepassword'");
        assertEquals("alicetest@gmail.com", userInDB.email());

        AuthData authInDB = authDAO.getAuth(result.authToken());
        assertNotNull(authInDB);
        assertEquals("aliceusername", authInDB.username());
    }

    @Test
    void testRegisterUserAlreadyTaken() throws DataAccessException {
        // Pre-insert user
        userDAO.createUser(new UserData("jamesusername", "jamespassword1", "jamesemail@gmail.com"));

        RegisterRequest request = new RegisterRequest("jamesusername", "jamespassword", "jamesemail@hotmail.com");

        // Expect an exception if we try to re-register same user
        assertThrows(DataAccessException.class, () -> {
            userService.register(request);
        });
    }

    @Test
    void testRegisterInvalidInput() {
        // Try with blank username
        RegisterRequest badRequest = new RegisterRequest("", "blank", "email@mail.com");
        assertThrows(DataAccessException.class, () -> {
            userService.register(badRequest);
        });
    }

    @Test
    void testLoginSuccess() throws DataAccessException {
        // Register user so they exist in the DAO
        RegisterRequest registerRequest = new RegisterRequest("amy", "amypass2", "amy@provider.com");
        RegisterResult registerResult = userService.register(registerRequest);
        assertNotNull(registerResult.authToken());

        // Attempt to log in w/ correct credentials
        LoginRequest loginRequest = new LoginRequest("amy", "amypass2");
        LoginResult loginResult = userService.login(loginRequest);

        // Verify login result
        assertNotNull(loginResult);
        assertEquals("amy", loginResult.username());
        assertNotNull(loginResult.authToken());

        // Check that new auth token was really created in the DAO
        AuthData authData = authDAO.getAuth(loginResult.authToken());
        assertNotNull(authData);
        assertEquals("amy", authData.username());
    }

    @Test
    void testLoginNoSuchUser() throws DataAccessException {
        // Attempt to log in with nonexistent user
        LoginRequest loginRequest = new LoginRequest("rob", "123123123");
        assertThrows(DataAccessException.class, () -> {
            userService.login(loginRequest);
        });
    }

    @Test
    void testLoginBadPassword() throws DataAccessException {
        // Register a user
        RegisterRequest registerRequest = new RegisterRequest("devin_", "password", "devindevin@example.com");
        userService.register(registerRequest);

        // Try to log in with wrong password
        LoginRequest badPasswordReq = new LoginRequest("devin_", "wrongPassword");
        assertThrows(DataAccessException.class, () -> {
            userService.login(badPasswordReq);
        });
    }

    @Test
    void testLoginMissingFields() {
        // Missing username
        LoginRequest badRequest1 = new LoginRequest(null, "testtest");
        // Missing password
        LoginRequest badRequest2 = new LoginRequest("dave", "");

        assertThrows(DataAccessException.class, () -> {
            userService.login(badRequest1);
        });
        assertThrows(DataAccessException.class, () -> {
            userService.login(badRequest2);
        });
    }

    @Test
    void testLogoutSuccess() throws DataAccessException {
        // Register/login first to get valid token
        RegisterResult regResult = userService.register(new RegisterRequest("john", "johnjohn", "a@b.com"));
        assertNotNull(regResult.authToken());

        // Ensure token is recognized by the DAO
        AuthData authData = authDAO.getAuth(regResult.authToken());
        assertNotNull(authData);

        // Log out
        LogoutRequest logoutReq = new LogoutRequest(regResult.authToken());
        LogoutResult logoutResult = userService.logout(logoutReq);

        // Check result
        assertNotNull(logoutResult);

        // Verify the token no longer exists
        AuthData authAfterLogout = authDAO.getAuth(regResult.authToken());
        assertNull(authAfterLogout, "Token should be removed after logout");
    }

    @Test
    void testLogoutInvalidToken() throws DataAccessException {
        // Attempt to log out w random/bogus token
        LogoutRequest request = new LogoutRequest("this_token_does_not_exist");

        // Expect exception
        assertThrows(DataAccessException.class, () -> {
            userService.logout(request);
        });
    }

    @Test
    void testLogoutMissingToken() {
        // Attempt to log out w blank or null
        LogoutRequest badRequest = new LogoutRequest("");
        assertThrows(DataAccessException.class, () -> {
            userService.logout(badRequest);
        });
    }
}

