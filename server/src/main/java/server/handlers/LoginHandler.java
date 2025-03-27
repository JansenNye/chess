package server.handlers;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.UserService;
import requests.LoginRequest;
import results.LoginResult;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * Handles [POST] /session
 * Body: {"username":"...", "password":"..."}
 * Returns: {"username":"...", "authToken":"..."} on success
 */
public class LoginHandler implements Route {

    private final UserService userService;
    private final Gson gson = new Gson();

    public LoginHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Object handle(Request request, Response response) {
        try {
            // Parse input from JSON
            LoginRequest loginReq = gson.fromJson(request.body(), LoginRequest.class);

            // Call service method
            LoginResult result = userService.login(loginReq);

            // Return 200 ,+ JSON result
            response.status(200);

            return gson.toJson(result);

        } catch (DataAccessException e) {
            // Decide which HTTP status to return
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();

            if (msg.contains("unauthorized") || msg.contains("incorrect password") || msg.contains("not exist")) {
                response.status(401);
                return gson.toJson(new ErrorMessage("Error: unauthorized"));
            } else if (msg.contains("bad request") || msg.contains("invalid")) {
                response.status(400);
                return gson.toJson(new ErrorMessage("Error: bad request"));
            } else {
                response.status(500);
                return gson.toJson(new ErrorMessage("Error: " + e.getMessage()));
            }
        } catch (Exception e) {
            response.status(500);
            return gson.toJson(new ErrorMessage("Error: " + e.getMessage()));
        }
    }
    // Empty
    private record ErrorMessage(String message) {}
}
