package server.handlers;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.UserService;
import service.requests.RegisterRequest;
import service.results.RegisterResult;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * Handles [POST] /user
 */
public class RegisterHandler implements Route {

    private final UserService userService;
    private final Gson gson = new Gson();

    public RegisterHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Object handle(Request request, Response response) {
        try {
            // Parse JSON body into RegisterRequest, call service
            RegisterRequest registerReq = gson.fromJson(request.body(), RegisterRequest.class);
            RegisterResult result = userService.register(registerReq);

            // On success, return 200 + JSON result
            response.status(200);
            return gson.toJson(result);

        } catch (DataAccessException e) {
            // 400 - bad request
            // 403 - already taken
            // otherwise 500
            String error = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (error.contains("invalid") || error.contains("bad request")) {
                response.status(400);
                return gson.toJson(new ErrorMessage("Error: bad request"));
            } else if (error.contains("already taken")) {
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

    // Empty
    private record ErrorMessage(String message) {}
}

