package server.handlers;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.GameService;
import requests.JoinGameRequest;
import results.JoinGameResult;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * Handles [PUT] /game
 * Headers: authorization: <authToken>
 * Body: {"playerColor":"WHITE/BLACK","gameID":1234}
 * Returns: {} on success
 */
public class JoinGameHandler implements Route {

    private final GameService gameService;
    private final Gson gson = new Gson();

    public JoinGameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public Object handle(Request request, Response response) {
        try {
            // Grab auth token from header
            String authToken = request.headers("authorization");
            if (authToken == null || authToken.isBlank()) {
                response.status(401);
                return gson.toJson(new ErrorMessage("Error: unauthorized"));
            }

            // Parse JSON body for playerColor and gameID
            BodyJSON body = gson.fromJson(request.body(), BodyJSON.class);
            if (body == null) {
                response.status(400);
                return gson.toJson(new ErrorMessage("Error: bad request (empty or invalid JSON)"));
            } if (body.playerColor == null || body.playerColor.isBlank()) {
                response.status(400);
                return gson.toJson(new ErrorMessage("Error: bad request (missing playerColor)"));
            } if (body.gameID <= 0) {
                response.status(400);
                return gson.toJson(new ErrorMessage("Error: bad request (invalid gameID)"));
            }

            // Build servicelayer request
            JoinGameRequest joinReq = new JoinGameRequest(authToken, body.playerColor, body.gameID);

            // Call joingame
            JoinGameResult result = gameService.joinGame(joinReq);

            // On success, return 200 + empty JSON
            response.status(200);
            return gson.toJson(new EmptyJson());

        } catch (DataAccessException e) {
            String msg = (e.getMessage() == null) ? "" : e.getMessage().toLowerCase();
            if (msg.contains("unauthorized") || msg.contains("invalid token")) {
                response.status(401);
                return gson.toJson(new ErrorMessage("Error: unauthorized"));
            } else if (msg.contains("bad request") || msg.contains("invalid player color") || msg.contains("game not found")) {
                response.status(400);
                return gson.toJson(new ErrorMessage("Error: bad request"));
            } else if (msg.contains("already taken")) {
                response.status(403);
                return gson.toJson(new ErrorMessage("Error: already taken"));
            } else {
                response.status(500);
                return gson.toJson(new ErrorMessage("Error: " + e.getMessage()));
            }
        } catch (Exception e) {
            response.status(500);
            return gson.toJson(new ErrorMessage("Error: " + e.getMessage()));
        }
    }

    /**
     * Helper record for parsing JSON from the request body
     */
    private record BodyJSON(String playerColor, int gameID) {}

    // Empty
    private record EmptyJson() {}

    // Empty
    private record ErrorMessage(String message) {}
}

