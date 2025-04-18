package server.handlers;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.UserService;
import requests.LogoutRequest;
import results.LogoutResult;
import spark.Request;
import spark.Response;
import spark.Route;

import static server.handlers.ListGamesHandler.jsonConvert;

/**
 * Handles [DELETE] /session
 * Headers: authorization: <authToken>
 * Returns: {} on success
 */
public class LogoutHandler implements Route {

    private final UserService userService;
    private final Gson gson = new Gson();

    public LogoutHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Object handle(Request request, Response response) {
        try {
            // Extract token from authorization header
            String token = request.headers("authorization");
            if (token == null || token.isBlank()) {
                response.status(401);
                return gson.toJson(new ErrorMessage("Error: unauthorized"));
            }

            // Create logoutRequest
            LogoutRequest logoutReq = new LogoutRequest(token);

            // Call service
            LogoutResult result = userService.logout(logoutReq);

            // On success return 200 + empty JSON object
            response.status(200);
            return gson.toJson(new EmptyJson());

        } catch (DataAccessException e) {
            return jsonConvert(e, response);
        } catch (Exception e) {
            response.status(500);
            return gson.toJson(new ErrorMessage("Error: " + e.getMessage()));
        }
    }
    // Empty
    private record EmptyJson() {}

    // EMpty
    private record ErrorMessage(String message) {}
}

