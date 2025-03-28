package ui;
import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;
import results.ListGamesResult;
import serverfacade.ServerFacade;
import exception.ResponseException;
import model.AuthData;
import model.GameData;

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
            return String.format("Created game '%s'", game.gameName());
        }
        throw new ResponseException(400, "Expected: create <game name>");
    }

    //List games
    private String listGames() throws ResponseException {
        ensureLoggedIn();

        List<ListGamesResult.GameInfo> games = server.listGames(authToken);
        if (games.isEmpty()) {
            return "No games available";
        }
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (ListGamesResult.GameInfo info : games) {
            String whitePlayer = info.whiteUsername() != null ? info.whiteUsername() : "None";
            String blackPlayer = info.blackUsername() != null ? info.blackUsername() : "None";
            sb.append(String.format("%d) %s | WHITE: %s | BLACK: %s\n",
                    index++,
                    info.gameName(),
                    whitePlayer,
                    blackPlayer));
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
        if (state == State.LOGGEDOUT) {
            throw new ResponseException(400, "Must be logged in");
        }
    }

    // Join game
    private String joinGame(String... p) throws ResponseException {
        ensureLoggedIn();
        if (p.length == 2) {
            int idx;
            try {
                idx = Integer.parseInt(p[0].trim()) - 1;
            } catch (NumberFormatException e) {
                throw new ResponseException(400, "Expected numeric index");
            }

            List<ListGamesResult.GameInfo> games = server.listGames(authToken);
            if (idx < 0 || idx >= games.size()) {
                throw new ResponseException(400, "Game at this index does not exist.");
            }

            ListGamesResult.GameInfo info = games.get(idx);
            server.joinGame(authToken, info.gameID(), p[1].toUpperCase());
            state = State.GAMESTATE;
            boolean flip = p[1].equalsIgnoreCase("BLACK");
            return drawBoard(new ChessGame(), flip);
        }
        throw new ResponseException(400, "Expected: join <index> <WHITE|BLACK>");
    }

    private String observeGame(String... p) throws ResponseException {
        ensureLoggedIn();
        if (p.length == 1) {
            int idx;
            try {
                idx = Integer.parseInt(p[0].trim()) - 1;
            } catch (NumberFormatException e) {
                throw new ResponseException(400, "Expected numeric index");
            }

            List<ListGamesResult.GameInfo> games = server.listGames(authToken);
            if (idx < 0 || idx >= games.size()) {
                throw new ResponseException(400, "Game at this index does not exist.");
            }

            state = State.OBSERVING;
            return drawBoard(new ChessGame(), false);
        }
        throw new ResponseException(400, "Expected: observe <index>");
    }

    private String drawBoard(ChessGame game, boolean flip) {
        ChessBoard board = game.getBoard();
        StringBuilder sb = new StringBuilder();

        // Determine iteration order based on flip
        int startRank = flip ? 1 : 8;
        int endRank   = flip ? 8 : 1;
        int stepRank  = flip ? 1 : -1;

        // Iterate through ranks
        for (int r = startRank; r != endRank + stepRank; r += stepRank) { // Nesting Level 1
            sb.append(SET_TEXT_COLOR_WHITE); // Set color for rank number
            sb.append(" ").append(r).append(" "); // Add rank number at the start of the row

            int startFile = flip ? 8 : 1;
            int endFile   = flip ? 1 : 8;
            int stepFile  = flip ? -1 : 1;

            // Iterate through files
            for (int f = startFile; f != endFile + stepFile; f += stepFile) {
                ChessPiece piece = board.getPiece(new ChessPosition(r, f));
                boolean isLightSquare = (r + f) % 2 == 0;
                String pieceStr = getPieceString(piece);

                // Append background color, piece string, and reset background
                sb.append(isLightSquare ? SET_BG_COLOR_DARK_GREY : SET_BG_COLOR_LIGHT_GREY);
                sb.append(pieceStr);
                sb.append(RESET_BG_COLOR);
            }

            sb.append(RESET_TEXT_COLOR);
            sb.append("\n");
        }

        // Append file letters footer
        sb.append(SET_TEXT_COLOR_WHITE); // Set color for file letters
        sb.append(flip
                ? "    h  g  f  e  d  c  b  a \n"
                : "    a  b  c  d  e  f  g  h \n"); // Adjusted spacing for alignment
        sb.append(RESET_TEXT_COLOR); // Reset color

        return sb.toString();
    }

    /**
     * Helper method to get the appropriate escape sequence string for a chess piece.
     * @param piece The chess piece (can be null).
     * @return The string representation (e.g., WHITE_KING, EMPTY).
     */
    private static String getPieceString(ChessPiece piece) {
        if (piece == null) {
            return EMPTY;
        }
        return switch (piece.getTeamColor()) {
            case WHITE -> switch (piece.getPieceType()) {
                // Level 3 cases
                case KING   -> BLACK_KING;
                case QUEEN  -> BLACK_QUEEN;
                case ROOK   -> BLACK_ROOK;
                case BISHOP -> BLACK_BISHOP;
                case KNIGHT -> BLACK_KNIGHT;
                case PAWN   -> BLACK_PAWN;
            };
            case BLACK -> switch (piece.getPieceType()) {
                case KING   -> WHITE_KING;
                case QUEEN  -> WHITE_QUEEN;
                case ROOK   -> WHITE_ROOK;
                case BISHOP -> WHITE_BISHOP;
                case KNIGHT -> WHITE_KNIGHT;
                case PAWN   -> WHITE_PAWN;
            };
        };
    }
}


