package ui;
import serverfacade.ServerFacade;
import exception.ResponseException;
import model.AuthData;
import model.GameData;
import service.results.ListGamesResult;
import service.results.ListGamesResult.GameInfo;

import java.util.Arrays;

import java.util.List;

public class ChessClient {
    private final ServerFacade server;
    private String authToken = null;
    private State state = State.LOGGEDOUT;

    public ChessClient(String serverUrl) {
        server = new ServerFacade(serverUrl);
    }

    // Evaluate input
    public String eval(String input) {
        try {
            var tokens = input.trim().split("\\s+");
            var params = Arrays.copyOfRange(tokens, 1, tokens.length);
            var cmd = tokens.length > 0 ? tokens[0].toLowerCase() : "help";

            return switch (cmd) {
                case "register" -> register(params);
                case "login" -> login(params);
                case "logout" -> logout();
                case "create" -> createGame(params);
                case "list" -> listGames();
                case "play", "join" -> joinGame(params);
                case "observe" -> observeGame(params);
                case "help" -> help();
                case "quit" -> "quit";
                default -> "No clue what that command is. Type help, slime";
            };
        }
        catch (ResponseException e) {
            return e.getMessage();
        }
    }

    // Register
    private String register(String... params) throws ResponseException {
        if (params.length == 3) {
            AuthData data = server.register(params[0], params[1], params[2]);
            authToken = data.authToken();
            state = State.LOGGEDIN;
            return "Registered & logged in as: " + data.username();
        } throw new ResponseException(400, " Expected: register <username> <password> <email>");
    }

    //Login
    private String login(String... params) throws ResponseException {
        if (params.length == 2) {
            AuthData data = server.login(params[0], params[1]);
            authToken = data.authToken();
            state = State.LOGGEDIN;
            return "Logged in as: " + data.username();
        }
        throw new ResponseException(400, "Expected: login <username> <password>");
    }

    //Logout
    private String logout() throws ResponseException {
        ensureLoggedIn();
        server.logout(authToken);
        authToken = null;
        state = State.LOGGEDOUT;
        return "Logged out.";
    }

    //Create game
    private String createGame(String... params) throws ResponseException {
        ensureLoggedIn();
        if (params.length == 1) {
            GameData game = server.createGame(authToken, params[0]);
            return String.format("Created game '%s' (ID: %d)", game.gameName(), game.gameID());
        }
        throw new ResponseException(400, "Expected: create <game name>");
    }

    //List games
    private String listGames() throws ResponseException {
        ensureLoggedIn();

        List<GameInfo> games = server.listGames(authToken);
        if (games.isEmpty()) return "No games available";
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (GameInfo info : games) {
            sb.append(String.format("%d) %s | Players: %s/%s\n", index++, info.gameName(),
                    info.whiteUsername() != null ? "1" : "0", info.blackUsername() != null ? "1" : "0"));
        }
        return sb.toString();
    }

    c

    private void ensureLoggedIn() throws ResponseException {
        if (state != State.LOGGEDIN) {
            throw new ResponseException(400, "Must be logged in");
        }
    }
}


