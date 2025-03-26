package ui;
import chess.ChessBoard;
import chess.ChessPiece;
import chess.ChessPosition;
import serverfacade.ServerFacade;
import exception.ResponseException;
import model.AuthData;
import model.GameData;
import service.results.ListGamesResult;
import service.results.ListGamesResult.GameInfo;

import java.util.Arrays;

import java.util.List;

import static ui.EscapeSequences.*;

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


    // help
    public String help() {
        if (state == State.LOGGEDOUT) {
            return "register <USERNAME> <PASSWORD> <EMAIL>\nlogin <USERNAME> <PASSWORD>\nquit";
        }
        return String.join("\n", new String[]{
                        "create <NAME>", "list", "join <INDEX> <WHITE|BLACK>", "observe <INDEX>", "logout", "quit"
                }
        );
    }

    private void ensureLoggedIn() throws ResponseException {
        if (state != State.LOGGEDIN) {
            throw new ResponseException(400, "Must be logged in");
        }
    }

    // Join game
    private String joinGame(String... params) throws ResponseException {
        ensureLoggedIn();
        if(params.length==2) {
            int idx=Integer.parseInt(params[0]);
            String color=params[1].toUpperCase();
            List<GameInfo> games=server.listGames(authToken);
            GameInfo info=games.get(idx-1);
            server.joinGame(authToken, info.gameID(),color);
            state=State.GAMESTATE;
            GameData data = server.createGame(authToken, info.gameName()); // fetch board?
            return drawBoard(data);
        }
        throw new ResponseException(400,"Expected: join <index> <WHITE|BLACK>");
    }

    private String observeGame(String... params) throws ResponseException {
        ensureLoggedIn();
        if(params.length==1) {
            int idx=Integer.parseInt(params[0]);
            GameInfo info=server.listGames(authToken).get(idx-1);
            state=State.OBSERVING;
            GameData data = server.createGame(authToken, info.gameName());
            return drawBoard(data);
        }
        throw new ResponseException(400,"Expected: observe <index>");
    }

    private String drawBoard(GameData data) {
        StringBuilder sb=new StringBuilder();
        ChessBoard board=data.game().getBoard(); // assume getBoard returns 2D array of piece codes
        for(int rank=7; rank>=0; rank--) {
            sb.append(rank+1).append(" ");
            for(int file=0; file<8; file++) {
                ChessPiece piece = board.getPiece(new ChessPosition(rank, file));
                sb.append(piece==null?EMPTY:piece);
            }
            sb.append("\n");
        }
        sb.append("  a  b  c  d  e  f  g  h\n");
        return sb.toString();
    }
}


