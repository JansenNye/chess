package server.handlers;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.GameService;
import service.requests.ListGamesRequest;
import service.results.ListGamesResult;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * Handles [GET] /game
 * Header: authorization: <authToken>
 * Returns: { "games": [ ... ] } on success
 */
public class ListGamesHandler implements Route {

    private final GameService gameService;
    private static final Gson gson = new Gson();

    public ListGamesHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public Object handle(Request request, Response response) {
        try {
            // Get auth token from authorization header
            String authToken = request.headers("authorization");
            if (authToken == null || authToken.isBlank()) {
                response.status(401);
                return gson.toJson(new ErrorMessage("Error: unauthorized"));
            }

            // Build ListGamesRequest &  call service
            ListGamesRequest req = new ListGamesRequest(authToken);
            ListGamesResult result = gameService.listGames(req);

            // Return 200 + JSON
            response.status(200);
            return gson.toJson(result);

        } catch (DataAccessException e) {
            return jsonConvert(e, response);

        } catch (Exception e) {
            response.status(500);
            return gson.toJson(new ErrorMessage("Error: " + e.getMessage()));
        }
    } // Empty

    public static Object jsonConvert(DataAccessException e, Response response) {
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (msg.contains("unauthorized") || msg.contains("invalid")) {
            response.status(401);
            return gson.toJson(new ErrorMessage("Error: unauthorized"));
        } else {
            response.status(500);
            return gson.toJson(new ErrorMessage("Error: " + e.getMessage()));
        }
    }

    private record ErrorMessage(String message) {}
}
