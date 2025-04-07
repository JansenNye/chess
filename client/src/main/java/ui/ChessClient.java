package ui;
import chess.*;
import exception.ResponseException;
import model.AuthData;
import model.GameData;
import results.ListGamesResult.GameInfo;
import websocket.WebSocketCommunicator;
import websocket.commands.*;
import websocket.messages.*;
import serverfacade.ServerFacade;

import java.util.*;

import static ui.EscapeSequences.*;
public class ChessClient implements ServerMessageObserver {
    private final ServerFacade server;
    private final String serverUrl; // Store server URL base
    private String authToken = null;
    private State state = State.LOGGEDOUT;
    private WebSocketCommunicator wsCommunicator = null; // WebSocket communicator instance
    private ChessGame currentGame = null; // Holds the current game state received from server
    private Integer currentGameID = null; // ID of the game being played/observed
    private ChessGame.TeamColor playerColor = null; // null for observer
    public ChessClient(String serverUrl) {
        this.serverUrl = serverUrl; // Store for WS connection
        this.server = new ServerFacade(serverUrl);
    }
    @Override
    public void notify(ServerMessage message) {
        switch (message.getServerMessageType()) {
            case LOAD_GAME:
                LoadGameMessage loadGameMsg = (LoadGameMessage) message;
                this.currentGame = loadGameMsg.getGame();
                System.out.println("\nGame loaded/updated.");
                drawBoardBasedOnColor();
                printPrompt(); // Show prompt again after drawing
                break;
            case ERROR:
                ErrorMessage errorMsg = (ErrorMessage) message;
                System.out.println(SET_TEXT_COLOR_RED + "\nServer Error. " + errorMsg.getErrorMessage() + RESET_TEXT_COLOR);
                printPrompt(); // Show prompt again
                break;
            case NOTIFICATION:
                NotificationMessage notificationMsg = (NotificationMessage) message;
                System.out.println(SET_TEXT_COLOR_BLUE + "\nNotification: " + notificationMsg.getMessage() + RESET_TEXT_COLOR);
                printPrompt(); // Show prompt again
                break;
            default:
                System.out.println(SET_TEXT_COLOR_RED + "\nUnknown message type received: " + message.getServerMessageType() + RESET_TEXT_COLOR);
                printPrompt();
                break;
        }
    }
    public String eval(String input) {
        try { var tokens = input.toLowerCase().trim().split("\\s+");
            var cmd = tokens.length > 0 ? tokens[0] : "help";
            var params = Arrays.copyOfRange(tokens, 1, tokens.length);
            return switch (state) {
                case LOGGEDOUT -> evalLoggedOut(cmd, params);
                case LOGGEDIN -> evalLoggedIn(cmd, params);
                case GAMESTATE -> evalGameplay(cmd, params);
                case OBSERVING -> evalObserving(cmd, params);
            };
        } catch (ResponseException e) {
            String prefix = "Error"; // Default prefix
            String serverDetails = e.getMessage() != null ? e.getMessage() : "No details provided by server.";

            int statusCode = e.getStatusCode(); // Replace with your actual method if different

            prefix = switch (statusCode) {
                case 400 -> "Invalid Request";     // Bad syntax, missing fields
                case 401 -> "Authentication Failed"; // Incorrect username/password, bad token
                case 403 -> "Action Not Allowed";  // e.g., trying to join a full game
                case 404 -> "Not Found";           // Resource doesn't exist (less common here)
                case 500 -> "Server Error";         // Problem on the server side
                default -> "HTTP Error (" + statusCode + ")"; // Handle unexpected codes
            };
            return SET_TEXT_COLOR_RED + prefix + ": " + serverDetails + RESET_TEXT_COLOR;
        } catch (Exception e) {
            return SET_TEXT_COLOR_RED + "Error" + RESET_TEXT_COLOR;
        }
    }
    private String evalLoggedOut(String cmd, String[] params) throws ResponseException {
        return switch (cmd) {
            case "register" -> register(params);
            case "login" -> login(params);
            case "help" -> help();
            case "quit" -> "quit";
            default -> SET_TEXT_COLOR_RED + "Unknown command. Available: register, login, help, quit" + RESET_TEXT_COLOR;
        };
    }
    private String evalLoggedIn(String cmd, String[] params) throws ResponseException {
        return switch (cmd) {
            case "create" -> createGame(params);
            case "list" -> listGames();
            case "join" -> joinGame(params);     // Transitions state
            case "observe" -> observeGame(params); // Transitions state
            case "logout" -> logout();           // Transitions state
            case "help" -> help();
            case "quit" -> "quit";
            default -> SET_TEXT_COLOR_RED + "Unknown command. Available: create, list, join, observe, logout, help, quit" + RESET_TEXT_COLOR;
        };
    }
    private String evalGameplay(String cmd, String[] params) {
        return switch (cmd) {
            case "move" -> makeMove(params);
            case "resign" -> resignGame(params);
            case "highlight" -> highlightMoves(params);
            case "redraw" -> redrawBoard();
            case "leave" -> leaveGame(); // Transitions state
            case "help" -> help();
            default -> SET_TEXT_COLOR_RED + "Unknown command. Available: move, resign, highlight, redraw, leave, help" + RESET_TEXT_COLOR;
        };
    }
    private String evalObserving(String cmd, String[] params) { // Removed throws
        return switch (cmd) {
            case "highlight" -> highlightMoves(params);
            case "redraw" -> redrawBoard();
            case "leave" -> leaveGame(); // Transitions state
            case "help" -> help();
            default -> SET_TEXT_COLOR_RED + "Unknown command. Available: highlight, redraw, leave, help" + RESET_TEXT_COLOR;
        };
    }
    private String register(String... params) throws ResponseException {
        if (params.length != 3) {
            throw new ResponseException(400, "Usage: register <username> <password> <email>");
        }  AuthData data = server.register(params[0], params[1], params[2]);
        authToken = data.authToken();
        state = State.LOGGEDIN; // Set state
        return SET_TEXT_COLOR_GREEN + "Registered & logged in as: " + data.username() + RESET_TEXT_COLOR;
    }
    private String login(String... params) throws ResponseException {
        if (params.length != 2) {
            throw new ResponseException(400, "Usage: login <username> <password>");
        } AuthData data = server.login(params[0], params[1]);
        authToken = data.authToken();
        state = State.LOGGEDIN; // Set state
        return SET_TEXT_COLOR_GREEN + "Logged in as: " + data.username() + RESET_TEXT_COLOR;
    }
    private String logout() throws ResponseException {
        ensureLoggedInOrInGame(); // Check if logged in or in a game state
        try { server.logout(authToken); // Call HTTP logout
        } catch (ResponseException e) {
            System.out.println(SET_TEXT_COLOR_YELLOW + "Note: Server logout failed, continuing client cleanup. " + e.getMessage() + RESET_TEXT_COLOR);
        } authToken = null;
        wsLeaveCleanup(); // Close WebSocket if connected, clear game state
        state = State.LOGGEDOUT; // Set state
        return SET_TEXT_COLOR_GREEN + "Logged out." + RESET_TEXT_COLOR;
    }
    private String createGame(String... params) throws ResponseException {
        ensureLoggedInState(); // Must be logged in, not in game
        if (params.length != 1) {
            throw new ResponseException(400, "Usage: create <game name>");
        } GameData game = server.createGame(authToken, params[0]);
        return String.format(SET_TEXT_COLOR_GREEN + "Created game '%s'" + RESET_TEXT_COLOR, game.gameName());
    }
    private String listGames() throws ResponseException {
        ensureLoggedInState(); // Must be logged in, not in game
        List<GameInfo> games = server.listGames(authToken);
        if (games == null || games.isEmpty()) {
            return "No games available";
        } StringBuilder sb = new StringBuilder();
        sb.append("Available Games:\n");
        int index = 1;
        for (GameInfo info : games) {
            String whitePlayer = info.whiteUsername() != null ? info.whiteUsername() : "None";
            String blackPlayer = info.blackUsername() != null ? info.blackUsername() : "None";
            sb.append(String.format("  %d) %s | WHITE: %s | BLACK: %s\n",
                    index++, info.gameName(), whitePlayer, blackPlayer));
        } return sb.toString();
    }
    private String joinGame(String... params) throws ResponseException {
        ensureLoggedInState(); // Must be logged in, not already in a game
        if (params.length != 2) {
            throw new ResponseException(400, "Usage: join <game_index> <WHITE|BLACK>");
        }  int idx; String playerColorStr = params[1].toUpperCase();
        try { idx = Integer.parseInt(params[0].trim()) - 1;
        } catch (NumberFormatException e) {
            throw new ResponseException(400, "Invalid game index: Must be a number.");
        } if (!("WHITE".equals(playerColorStr) || "BLACK".equals(playerColorStr))) {
            throw new ResponseException(400, "Invalid color. Choose WHITE or BLACK.");
        }  this.playerColor = ChessGame.TeamColor.valueOf(playerColorStr);
        List<GameInfo> games = server.listGames(authToken);
        if (games == null || idx < 0 || idx >= games.size()) {
            this.playerColor = null; // Reset color if index is bad
            throw new ResponseException(400, "Invalid game index.");
        } GameInfo info = games.get(idx);
        this.currentGameID = info.gameID(); // Store gameID
        try { server.joinGame(authToken, this.currentGameID, playerColorStr);
        } catch (ResponseException e) {
            this.playerColor = null; // Reset state if join fails
            this.currentGameID = null;
            throw e;
        } try {
            String wsUrl = serverUrl.replaceFirst("http", "ws");
            wsCommunicator = new WebSocketCommunicator(wsUrl, this);
            ConnectCommand connectCmd = new ConnectCommand(authToken, this.currentGameID);
            wsCommunicator.sendMessage(connectCmd);
            state = State.GAMESTATE;
            return String.format(SET_TEXT_COLOR_GREEN + "Joining game %d as %s. " + RESET_TEXT_COLOR, this.currentGameID, playerColorStr);
        } catch (Exception e) {
            wsLeaveCleanup();
            state = State.LOGGEDIN; // Revert state
            throw new ResponseException(500, "Failed to connect to game server.");
        }
    }
    private String observeGame(String... params) throws ResponseException {
        ensureLoggedInState(); // Must be logged in, not already in a game
        if (params.length != 1) {
            throw new ResponseException(400, "Usage: observe <game_index>");
        } int idx;
        try { idx = Integer.parseInt(params[0].trim()) - 1;
        } catch (NumberFormatException e) {
            throw new ResponseException(400, "Invalid game index: Must be a number.");
        } this.playerColor = null; // Observer has no color
        List<GameInfo> games = server.listGames(authToken);
        if (games == null || idx < 0 || idx >= games.size()) {
            throw new ResponseException(400, "Invalid game index.");
        } GameInfo info = games.get(idx);
        this.currentGameID = info.gameID(); // Store gameID
        try {
            String wsUrl = serverUrl.replaceFirst("http", "ws");
            wsCommunicator = new WebSocketCommunicator(wsUrl, this);
            ConnectCommand connectCmd = new ConnectCommand(authToken, this.currentGameID);
            wsCommunicator.sendMessage(connectCmd);
            state = State.OBSERVING; // Change state
            return String.format(SET_TEXT_COLOR_GREEN + "Observing game %s. " + RESET_TEXT_COLOR, info.gameName());
        } catch (Exception e) {
            wsLeaveCleanup(); // Clean up if WebSocket connection or initial send fails
            state = State.LOGGEDIN; // Revert state
            throw new ResponseException(500, "Failed to connect to game server.");
        }
    }
    private String redrawBoard() {
        ensureInGameState(); // Check if in a game state
        if (currentGame == null) {
            return SET_TEXT_COLOR_YELLOW + "No game data loaded yet." + RESET_TEXT_COLOR;
        } drawBoardBasedOnColor();
        return ""; // Board drawing handles newlines/output
    }
    private String leaveGame() {
        ensureInGameState();
        try {
            if (wsCommunicator != null && wsCommunicator.isOpen()) {
                LeaveCommand leaveCmd = new LeaveCommand(authToken, currentGameID);
                wsCommunicator.sendMessage(leaveCmd);
            } wsLeaveCleanup();
            state = State.LOGGEDIN; // Transition
            return SET_TEXT_COLOR_GREEN + "You left the game." + RESET_TEXT_COLOR;
        } catch (Exception e) {
            wsLeaveCleanup();
            state = State.LOGGEDIN;
            return SET_TEXT_COLOR_YELLOW + "Error sending leave command, but you have left locally" + RESET_TEXT_COLOR;
        }
    }
    private String makeMove(String... params) {
        ensurePlayingState(); // Must be playing, not observing
        if (currentGame == null) {
            return SET_TEXT_COLOR_YELLOW + "Game not loaded yet." + RESET_TEXT_COLOR;
        } if (params.length < 2 || params.length > 3) {
            return SET_TEXT_COLOR_RED + "Usage: move <startPos> <endPos> [promotionPiece]" + RESET_TEXT_COLOR;
        } String startPosStr = params[0];
        String endPosStr = params[1];
        ChessPiece.PieceType promotionType = null;
        if (params.length == 3) {
            try {  // Allow short notation (q, r, b, n) or full name
                promotionType = switch(params[2].toUpperCase()) {
                    case "Q", "QUEEN" -> ChessPiece.PieceType.QUEEN;
                    case "R", "ROOK" -> ChessPiece.PieceType.ROOK;
                    case "B", "BISHOP" -> ChessPiece.PieceType.BISHOP;
                    case "N", "KNIGHT" -> ChessPiece.PieceType.KNIGHT;
                    default -> throw new IllegalArgumentException();
                };
            } catch (IllegalArgumentException e) {
                return SET_TEXT_COLOR_RED + "Invalid promotion piece. Use Q, R, B, or N (or QUEEN, etc.)." + RESET_TEXT_COLOR;
            }
        } try {
            ChessPosition startPos = parsePosition(startPosStr);
            ChessPosition endPos = parsePosition(endPosStr);
            if (currentGame.getTeamTurn() != this.playerColor) {
                return SET_TEXT_COLOR_RED + "It's not your turn." + RESET_TEXT_COLOR;
            } ChessPiece pieceToMove = currentGame.getBoard().getPiece(startPos);
            if (pieceToMove == null) {
                return SET_TEXT_COLOR_RED + "No piece at starting position " + startPosStr + "." + RESET_TEXT_COLOR;
            } if (pieceToMove.getTeamColor() != this.playerColor) {
                return SET_TEXT_COLOR_RED + "You cannot move opponent's piece at " + startPosStr + "." + RESET_TEXT_COLOR;
            } ChessMove move = new ChessMove(startPos, endPos, promotionType);
            MakeMoveCommand moveCmd = new MakeMoveCommand(authToken, currentGameID, move);
            wsCommunicator.sendMessage(moveCmd);
            return "";
        } catch (IllegalArgumentException e) {
            return SET_TEXT_COLOR_RED + "Invalid position format. Use algebraic notation (e.g., 'a1', 'h8')." + RESET_TEXT_COLOR;
        } catch (Exception e) {
            return SET_TEXT_COLOR_RED + "Error sending move. " + RESET_TEXT_COLOR;
        }
    }
    private String resignGame(String... params) {
        ensurePlayingState(); // Must be playing to resign
        System.out.print(SET_TEXT_COLOR_YELLOW + "Are you sure you want to resign? (yes/no): " + RESET_TEXT_COLOR);
        Scanner scanner = new Scanner(System.in);
        String confirmation = "";
        if (scanner.hasNextLine()) {
            confirmation = scanner.nextLine().trim().toLowerCase();
        } if (!"yes".equals(confirmation)) {
            printPrompt();
            return SET_TEXT_COLOR_GREEN + "Resignation cancelled." + RESET_TEXT_COLOR;
        } try {
            ResignCommand resignCmd = new ResignCommand(authToken, currentGameID);
            wsCommunicator.sendMessage(resignCmd);
            return "You have resigned. Type 'leave' to exit game view.";
        } catch (Exception e) {
            return SET_TEXT_COLOR_RED + "Error sending resignation. " + RESET_TEXT_COLOR;
        }
    }
    private String highlightMoves(String... params) {
        ensureInGameState(); // Can highlight when playing or observing
        if (currentGame == null) {
            return SET_TEXT_COLOR_YELLOW + "Game not loaded yet." + RESET_TEXT_COLOR;
        }  if (params.length != 1) {
            return SET_TEXT_COLOR_RED + "Usage: highlight <position> (e.g., highlight e2)" + RESET_TEXT_COLOR;
        } try {
            ChessPosition startPos = parsePosition(params[0]);
            ChessPiece piece = currentGame.getBoard().getPiece(startPos);
            if (piece == null) {
                return SET_TEXT_COLOR_YELLOW + "No piece at " + params[0] + "." + RESET_TEXT_COLOR;
            } Collection<ChessMove> validMoves = currentGame.validMoves(startPos);
            if (validMoves == null || validMoves.isEmpty()) {
                drawBoardWithHighlights(currentGame.getBoard(), startPos, null);
                return SET_TEXT_COLOR_YELLOW + "No legal moves for piece at " + params[0] + "." + RESET_TEXT_COLOR;
            } drawBoardWithHighlights(currentGame.getBoard(), startPos, validMoves);
            return ""; // Don't print extra message, board is the output
        } catch (IllegalArgumentException e) {
            return SET_TEXT_COLOR_RED + "Invalid position format. Use algebraic notation (e.g., 'a1', 'h8')." + RESET_TEXT_COLOR;
        } catch (Exception e) { // Catch potential errors from validMoves
            return SET_TEXT_COLOR_RED + "Error calculating moves." + RESET_TEXT_COLOR;
        }
    }
    private void ensureLoggedInState() throws ResponseException {
        if (state == State.LOGGEDOUT) {
            throw new ResponseException(400, "You must be logged in.");
        } if (state == State.GAMESTATE || state == State.OBSERVING) {
            throw new ResponseException(400, "You are already in a game. Type 'leave' first.");
        }
    }
    private void ensureInGameState() {
        if (state != State.GAMESTATE && state != State.OBSERVING) {
            throw new IllegalStateException("You must be in a game to perform this action.");
        }
    }
    private void ensurePlayingState() {
        if (state != State.GAMESTATE) {
            throw new IllegalStateException("You must be playing (not observing) to perform this action.");
        }
    }
    private void ensureLoggedInOrInGame() throws ResponseException {
        if (state == State.LOGGEDOUT) {
            throw new ResponseException(400, "You are not logged in.");
        }
    }
    private void wsLeaveCleanup() {
        if (wsCommunicator != null) {
            try {
                if (wsCommunicator.isOpen()) {
                    wsCommunicator.close();
                }
            } catch (Exception e) {
                System.err.println("Error closing WebSocket. ");
            }
        } wsCommunicator = null;
        currentGame = null;
        currentGameID = null;
        playerColor = null;
    }

    private void drawBoardBasedOnColor() {
        if (currentGame != null) {
            boolean flip = (this.playerColor == ChessGame.TeamColor.BLACK);
            System.out.print(drawBoard(currentGame.getBoard(), flip, null, null)); // No highlights
        } else {
            System.out.println(SET_TEXT_COLOR_YELLOW + "No game data to draw." + RESET_TEXT_COLOR);
        }
    }

    private void drawBoardWithHighlights(ChessBoard board, ChessPosition highlightSource, Collection<ChessMove> validMoves) {
        boolean flip = (this.playerColor == ChessGame.TeamColor.BLACK);
        Set<ChessPosition> highlightTargets = new HashSet<>();
        if (validMoves != null) {
            for (ChessMove move : validMoves) {
                highlightTargets.add(move.getEndPosition());
            }
        } System.out.print(drawBoard(board, flip, highlightSource, highlightTargets));
    }

    private String drawBoard(ChessBoard board, boolean flip, ChessPosition highlightSource, Set<ChessPosition> highlightTargets) {
        StringBuilder sb = new StringBuilder();
        int startRank = flip ? 1 : 8;
        int endRank   = flip ? 8 : 1;
        int stepRank  = flip ? 1 : -1;
        sb.append(SET_TEXT_COLOR_WHITE);
        sb.append(flip ? "    h  g  f  e  d  c  b  a \n" : "    a  b  c  d  e  f  g  h \n");
        sb.append(RESET_TEXT_COLOR);
        for (int r = startRank; r != endRank + stepRank; r += stepRank) {
            sb.append(SET_TEXT_COLOR_WHITE); // Color for rank number
            sb.append(" ").append(r).append(" "); // Rank number
            sb.append(RESET_TEXT_COLOR);
            int startFile = flip ? 8 : 1;
            int endFile   = flip ? 1 : 8;
            int stepFile  = flip ? -1 : 1;

            for (int f = startFile; f != endFile + stepFile; f += stepFile) {
                ChessPosition currentPos = new ChessPosition(r, f);
                ChessPiece piece = board.getPiece(currentPos);
                boolean isLightSquare = (r + f) % 2 != 0;
                String pieceStr = getPieceString(piece); // Gets piece symbol with text color codes
                String bgColor;
                if (highlightSource != null && highlightSource.equals(currentPos)) {
                    bgColor = SET_BG_COLOR_YELLOW;
                } else if (highlightTargets != null && highlightTargets.contains(currentPos)) {
                    bgColor = SET_BG_COLOR_GREEN;
                } else { // Standard checkerboard pattern
                    bgColor = isLightSquare ? SET_BG_COLOR_LIGHT_GREY : SET_BG_COLOR_DARK_GREY;
                } sb.append(bgColor);
                sb.append(pieceStr); // Append piece string (contains text color)
                sb.append(RESET_BG_COLOR); // Reset background for next square
            } sb.append(SET_TEXT_COLOR_WHITE); // Color for rank number
            sb.append(" ").append(r).append(" "); // Rank number again on the right
            sb.append(RESET_TEXT_COLOR);
            sb.append("\n"); // Newline for next rank
        } sb.append(SET_TEXT_COLOR_WHITE);
        sb.append(flip ? "    h  g  f  e  d  c  b  a \n" : "    a  b  c  d  e  f  g  h \n");
        sb.append(RESET_TEXT_COLOR);
        return sb.toString();
    }

    private static String getPieceString(ChessPiece piece) {
        if (piece == null) {
            return EMPTY; // EMPTY has padding
        } return switch (piece.getTeamColor()) {
            case WHITE -> switch (piece.getPieceType()) { // White pieces look black
                case KING   -> BLACK_KING;
                case QUEEN  -> BLACK_QUEEN;
                case ROOK   -> BLACK_ROOK;
                case BISHOP -> BLACK_BISHOP;
                case PAWN   -> BLACK_PAWN;
                case KNIGHT -> BLACK_KNIGHT;
            }; case BLACK -> switch (piece.getPieceType()) {  // Black pieces look white
                case KING   -> WHITE_KING;
                case QUEEN  -> WHITE_QUEEN;
                case ROOK   -> WHITE_ROOK;
                case BISHOP -> WHITE_BISHOP;
                case KNIGHT -> WHITE_KNIGHT;
                case PAWN   -> WHITE_PAWN;
            };
        };
    }

    public String help() {
        return switch (state) {
            case LOGGEDOUT -> """
                     register <USERNAME> <PASSWORD> <EMAIL> - Create an account
                     login <USERNAME> <PASSWORD>            - Log in
                     quit                                   - Exit Chess
                     help                                   - Show this help message
                     """;
            case LOGGEDIN -> """
                     create <NAME>          - Create a game
                     list                   - List available games
                     join <INDEX> <COLOR>   - Join a game as WHITE or BLACK (e.g., join 1 white)
                     observe <INDEX>        - Observe a game (e.g., observe 1)
                     logout                 - Log out
                     quit                   - Exit Chess
                     help                   - Show this help message
                     """;
            case GAMESTATE -> """
                     move <START> <END> [PROMOTION] - Make a move (e.g., move e2 e4, move e7 e8 q)
                     highlight <POSITION>           - Show legal moves for piece at POS (e.g., highlight e2)
                     redraw                         - Redraw the chess board
                     leave                          - Leave the current game
                     resign                         - Forfeit the game
                     help                           - Show this help message
                     """;
            case OBSERVING -> """
                     highlight <POSITION>           - Show legal moves for piece at POS (e.g., highlight e2)
                     redraw                         - Redraw the chess board
                     leave                          - Stop observing the game
                     help                           - Show this help message
                     """;
        };
    }

    private ChessPosition parsePosition(String posStr) throws IllegalArgumentException {
        if (posStr == null || posStr.length() != 2) {
            throw new IllegalArgumentException("Invalid position format (e.g., 'a1', 'h8').");
        } char fileChar = posStr.toLowerCase().charAt(0);
        char rankChar = posStr.charAt(1);

        if (fileChar < 'a' || fileChar > 'h' || rankChar < '1' || rankChar > '8') {
            throw new IllegalArgumentException("Position out of bounds ('a1'-'h8').");
        }  // ChessPosition(row, col) where row is 1-8 and col is 1-8 (a=1, h=8)
        int row = rankChar - '1' + 1;
        int col = fileChar - 'a' + 1;
        return new ChessPosition(row, col);
    }
    private void printPrompt() {
        System.out.print("\n" + RESET_TEXT_COLOR + SET_TEXT_BOLD + "[" + state + "] >>> " + RESET_TEXT_COLOR + SET_TEXT_COLOR_GREEN);
    }
}