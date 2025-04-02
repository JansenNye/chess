package server;

import dataaccess.*;
import server.handlers.*;
import service.ClearService;
import service.GameService;
import service.UserService;
import spark.Spark;

import dataaccess.DatabaseManager;
import websocket.WebSocketHandler;

public class Server {

    public int run(int desiredPort) {
        try {
            DatabaseManager.createDatabase();
            DatabaseManager.createTablesIfNotExists();
        } catch (DataAccessException e) {
            e.printStackTrace();
            System.err.println("Failed to create the database. Aborting server startup.");
            return -1;
        }

        // Set up Spark
        Spark.port(desiredPort);
        Spark.staticFiles.location("web");

        // Instantiate DAOs
        var userDAO = new UserDAOMySQL();
        var gameDAO = new GameDAOMySQL();
        var authDAO = new AuthDAOMySQL();

        // Instantiate Services
        var clearService = new ClearService(userDAO, gameDAO, authDAO);
        var userService = new UserService(userDAO, authDAO);
        var gameService = new GameService(gameDAO, authDAO);

        // Instantiate Handlers
        var clearHandler = new ClearHandler(clearService);
        var registerHandler = new RegisterHandler(userService);
        var loginHandler = new LoginHandler(userService);
        var logoutHandler = new LogoutHandler(userService);
        var listGamesHandler = new ListGamesHandler(gameService);
        var createGameHandler = new CreateGameHandler(gameService);
        var joinGameHandler = new JoinGameHandler(gameService);

        // Register Routes
        Spark.delete("/db", clearHandler);
        Spark.post("/user", registerHandler);
        Spark.post("/session", loginHandler);
        Spark.delete("/session", logoutHandler);
        Spark.get("/game", listGamesHandler);
        Spark.post("/game", createGameHandler);
        Spark.put("/game", joinGameHandler);
        Spark.webSocket("/ws", WebSocketHandler.class);

        // Wait for Spark to finish initialization
        Spark.awaitInitialization();
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}
