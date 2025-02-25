package server.handlers;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.GameService;
import service.requests.CreateGameRequest;
import service.results.CreateGameResult;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * Handles [POST] /game
 * Headers: authorization: <authToken>
 * Body: {"gameName":"..."}
 * Returns: {"gameID": 1234} on success
 */
public class CreateGameHandler implements Route {

    private final GameService gameService;
    private final Gson gson = new Gson();

    public CreateGameHandler(GameService gameService) {
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

            // Parse request body for gameName
            BodyJSON body = gson.fromJson(request.body(), BodyJSON.class);
            if (body == null || body.gameName == null || body.gameName.isBlank()) {
                response.status(400);
                return gson.toJson(new ErrorMessage("Error: bad request (missing gameName)"));
            }

            // Create service-layer request
            CreateGameRequest createReq = new CreateGameRequest(authToken, body.gameName);

            // Call service
            CreateGameResult result = gameService.createGame(createReq);

            // On success return 200 + JSON with gameID
            response.status(200);
            return gson.toJson(result);

        } catch (DataAccessException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("unauthorized") || msg.contains("invalid token")) {
                response.status(401);
                return gson.toJson(new ErrorMessage("Error: unauthorized"));
            } else if (msg.contains("bad request") || msg.contains("missing gamename")) {
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

    /**
     * Helper record for parsing incoming JSON body
     */
    private record BodyJSON(String gameName) {}

    // Empty
    private record ErrorMessage(String message) {}
}

