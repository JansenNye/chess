package server;
import dataaccess.*;
import server.handlers.*;
import service.ClearService;
import service.GameService;
import service.UserService;
import spark.*;

public class Server {

    public int run(int desiredPort) {
        Spark.port(desiredPort);
        Spark.staticFiles.location("web");

        // Register your endpoints and handle exceptions here.
        var userDAO = new UserDAOMySQL();
        var gameDAO = new GameDAOMySQL();
        var authDAO = new AuthDAOMySQL();

        // Services
        var clearService = new ClearService(userDAO, gameDAO, authDAO);
        var userService = new UserService(userDAO, authDAO);
        var gameService = new GameService(gameDAO, authDAO);

        // Handlers
        var clearHandler = new ClearHandler(clearService);
        var registerHandler = new RegisterHandler(userService);
        var loginHandler = new LoginHandler(userService);
        var logoutHandler = new LogoutHandler(userService);
        var listGamesHandler = new ListGamesHandler(gameService);
        var createGameHandler = new CreateGameHandler(gameService);
        var joinGameHandler = new JoinGameHandler(gameService);

        // Endpoints/routes
        Spark.delete("/db", clearHandler);
        Spark.post("/user", registerHandler);
        Spark.post("/session", loginHandler);
        Spark.delete("/session", logoutHandler);
        Spark.get("/game", listGamesHandler);
        Spark.post("/game", createGameHandler);
        Spark.put("/game", joinGameHandler);

        Spark.awaitInitialization();
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}
