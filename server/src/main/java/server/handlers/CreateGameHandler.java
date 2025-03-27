package server.handlers;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import model.GameData; // <-- Added import
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
 * Returns: Full GameData JSON on success {"gameID": 1234, "whiteUsername":null, ...}
 */
public class CreateGameHandler implements Route {

    private final GameService gameService;
    private final Gson gson = new Gson();

    public CreateGameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public Object handle(Request request, Response response) {
        GameData fetchedGame = null; // Variable to hold the full game data after creation
        try {
            // 1. Grab auth token from header
            String authToken = request.headers("authorization");
            if (authToken == null || authToken.isBlank()) {
                response.status(401); // Unauthorized
                return gson.toJson(new ErrorMessage("Error: unauthorized"));
            }

            // 2. Parse request body for gameName
            BodyJSON body = gson.fromJson(request.body(), BodyJSON.class);
            // Basic validation for JSON parsing and required field
            if (body == null || body.gameName == null || body.gameName.isBlank()) {
                response.status(400); // Bad Request
                return gson.toJson(new ErrorMessage("Error: bad request (missing gameName)"));
            }

            // 3. Create service-layer request object
            // Assuming CreateGameRequest definition is: (String authToken, String gameName)
            CreateGameRequest createReq = new CreateGameRequest(authToken, body.gameName);

            // 4. Call the service to create the game
            // This returns only the gameID according to your GameService code
            CreateGameResult result = gameService.createGame(createReq);

            // --- 5. FETCH THE NEWLY CREATED GAME DATA ---
            // Now use the gameID from the result to get the full GameData object
            try {
                // Use the new method added to GameService
                fetchedGame = gameService.getGameById(result.gameID());
                // We shouldn't get null here if creation succeeded, but check defensively
                if (fetchedGame == null) {
                    throw new Exception("Consistency error: Game not found immediately after creation, ID: " + result.gameID());
                }
            } catch (DataAccessException dae) {
                // If getGameById throws DataAccessException (e.g., DB error during fetch)
                System.err.println("Error fetching game data after creation: " + dae.getMessage());
                throw new DataAccessException("Server error retrieving created game details");
            } catch (Exception e) {
                // Catch other potential errors during fetch
                System.err.println("CRITICAL: Unexpected error fetching game after creation: " + e.getMessage());
                e.printStackTrace(); // Log the stack trace
                throw new DataAccessException("Unexpected server error retrieving game details");
            }
            // --- END FETCH ---

            // 6. On success, return 200 OK + JSON of the *full* GameData
            response.status(200);
            response.type("application/json"); // Explicitly set content type
            return gson.toJson(fetchedGame); // Return the full game data

            // Catch block specifically for DataAccessExceptions from gameService calls
        } catch (DataAccessException e) {
            return handleDataAccessException(e, response); // Use helper method

            // Catch potential JSON parsing errors or other unexpected exceptions
        } catch (Exception e) {
            response.status(500); // Internal Server Error
            // Log the exception internally for server-side debugging
            System.err.println("Internal Server Error in CreateGameHandler: " + e.getMessage());
            e.printStackTrace(); // Print stack trace to server logs
            response.type("application/json");
            return gson.toJson(new ErrorMessage("Error: Internal server error - " + e.getMessage()));
        }
    }

    /**
     * Helper method to handle common DataAccessExceptions and set response status/body.
     * @param e The exception caught.
     * @param response The Spark response object.
     * @return JSON string representing the error message.
     */
    private String handleDataAccessException(DataAccessException e, Response response) {
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        String errorType = "Error: data access"; // Default error message prefix

        if (msg.contains("unauthorized") || msg.contains("invalid token")) {
            response.status(401); // Unauthorized
            errorType = "Error: unauthorized";
        } else if (msg.contains("bad request") || msg.contains("missing gamename")) {
            response.status(400); // Bad Request
            errorType = "Error: bad request";
        } else {
            response.status(500); // Internal Server Error (likely DB issue)
            errorType = "Error: internal server";
            // Log the exception internally for server-side debugging
            System.err.println("Internal Server Error (DataAccessException) in CreateGameHandler: " + e.getMessage());
            e.printStackTrace(); // Print stack trace to server logs
        }
        response.type("application/json");
        return gson.toJson(new ErrorMessage(errorType)); // Return generic message for security
    }


    /**
     * Helper record for parsing incoming JSON body {"gameName":"..."}
     */
    private record BodyJSON(String gameName) {}

    /**
     * Helper record for returning error messages in JSON format {"message":"..."}
     */
    private record ErrorMessage(String message) {}
}