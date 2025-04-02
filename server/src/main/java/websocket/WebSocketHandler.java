package websocket;

import chess.ChessMove;
import chess.InvalidMoveException;
import com.google.gson.Gson;
import dataaccess.*;
import model.AuthData;
import model.GameData;
import model.GameStatus;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import service.GameService; // If needed for game logic/validation
import websocket.commands.*;
import websocket.messages.*;
import chess.ChessGame;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class WebSocketHandler {

    private final AuthDAO authDAO;
    private final GameDAO gameDAO;
    // private final GameService gameService;

    // Connection management (needs refinement)
    private final ConnectionManager connectionManager;
    private final Gson gson = new Gson();

    public WebSocketHandler() {
        this.authDAO = new AuthDAOMySQL();
        this.gameDAO = new GameDAOMySQL();
        // this.gameService = new GameService(gameDAO, authDAO);
        this.connectionManager = new ConnectionManager(); // Instantiate connection manager
    }


    @OnWebSocketConnect
    public void onConnect(Session session) throws IOException {
        System.out.println("WS Connect: " + session.getRemoteAddress());
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("WS Close: " + session.getRemoteAddress() + " Code: " + statusCode + " Reason: " + reason);
        connectionManager.removeSessionGlobally(session); // Use improved removal
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        System.err.println("WS Error on session " + (session != null ? session.getRemoteAddress() : "unknown") + ": " + error.getMessage());
        error.printStackTrace();
        if (session != null) {
            connectionManager.removeSessionGlobally(session); // Clean up on error
        }
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String messageJson) throws IOException {
        UserGameCommand baseCommand = null;
        try {
            baseCommand = gson.fromJson(messageJson, UserGameCommand.class);
            String authToken = baseCommand.getAuthToken();
            AuthData authData = authDAO.getAuth(authToken); // Validate token

            if (authData == null) {
                sendError(session, "Authentication failed: Invalid or expired token.");
                return; // Stop processing if not authenticated
            }

            String username = authData.username();
            switch (baseCommand.getCommandType()) {
                case CONNECT -> handleConnect(session, messageJson, username); // Pass username
                case MAKE_MOVE -> handleMakeMove(session, messageJson, username);
                case LEAVE -> handleLeave(session, messageJson, username);
                case RESIGN -> handleResign(session, messageJson, username);
                default -> sendError(session, "Unknown command type received.");
            }

        } catch (DataAccessException dbError) {
            sendError(session, "A database error occurred: " + dbError.getMessage());
        } catch (InvalidMoveException moveError) {
            sendError(session, "Invalid move: " + moveError.getMessage());
        } catch (Exception e) { // Catch JSON parsing errors or other issues
            System.err.println("Error processing message: " + e.getMessage());
            e.printStackTrace();
            String errorMsg = "Error processing request";
            if (baseCommand != null) {
                errorMsg += " for command: " + baseCommand.getCommandType();
            }
            sendError(session, errorMsg + ". Details: " + e.getMessage());
        }
    }

    private void handleConnect(Session session, String messageJson, String username) throws DataAccessException, IOException {
        System.out.println("Handling CONNECT for user: " + username);
        ConnectCommand connectCmd = gson.fromJson(messageJson, ConnectCommand.class);
        Integer gameID = connectCmd.getGameID();

        if (gameID == null) {
            sendError(session, "Error: Game ID is required for CONNECT command.");
            return;
        }

        // Validate Game Exists
        GameData gameData = gameDAO.getGame(gameID);
        if (gameData == null) {
            sendError(session, "Error: Game with ID " + gameID + " not found.");
            return;
        }

        // Determine player role (check if user is white/black player in the game)
        ChessGame.TeamColor userColor = null;
        if (username.equals(gameData.whiteUsername())) {
            userColor = ChessGame.TeamColor.WHITE;
        } else if (username.equals(gameData.blackUsername())) {
            userColor = ChessGame.TeamColor.BLACK;
        }
        String role = (userColor != null) ? userColor.toString() : "observer";

        // Add session to the connection manager
        connectionManager.add(gameID, session, username); // Store username with session

        // Send LOAD_GAME message back to connecting client (root client)
        LoadGameMessage loadGameMsg = new LoadGameMessage(gameData.game());
        session.getRemote().sendString(gson.toJson(loadGameMsg));
        System.out.println("Sent LOAD_GAME to " + username);

        // Send NOTIFICATION message to all other clients in the game
        String notificationText = String.format("%s joined the game as %s.", username, role);
        NotificationMessage notification = new NotificationMessage(notificationText);
        connectionManager.broadcast(gameID, session, gson.toJson(notification)); // Exclude self
    }

    private void handleMakeMove(Session session, String messageJson, String username) throws DataAccessException, IOException, InvalidMoveException {
        System.out.println("Handling MAKE_MOVE for user: " + username);
        MakeMoveCommand moveCmd = gson.fromJson(messageJson, MakeMoveCommand.class);
        Integer gameID = moveCmd.getGameID();
        ChessMove move = moveCmd.getMove();
        if (gameID == null || move == null) {
            sendError(session, "Error: Missing game ID or move data.");
            return;
        }

        // Get Game Data
        GameData gameData = gameDAO.getGame(gameID);
        if (gameData == null) {
            sendError(session, "Error: Game " + gameID + " not found.");
            return;
        }

        // Check game status
        if (gameData.status() != GameStatus.ACTIVE) {
            sendError(session, "Error: Game is already over (" + gameData.status() + ").");
            return;
        }

        // Check if user is an observer
        ChessGame.TeamColor playerColor = null;
        if (username.equals(gameData.whiteUsername())) {
            playerColor = ChessGame.TeamColor.WHITE;
        } else if (username.equals(gameData.blackUsername())) {
            playerColor = ChessGame.TeamColor.BLACK;
        }
        if (playerColor == null) {
            sendError(session, "Error: Observers cannot make moves.");
            return;
        }

        // 3. Check if it's the user's turn
        ChessGame currentGame = gameData.game();
        if (currentGame.getTeamTurn() != playerColor) {
            sendError(session, "Error: It is not your turn.");
            return;
        }

        // 4. Attempt to make the move using ChessGame logic
        currentGame.makeMove(move);

        ChessGame.TeamColor opponentColor = (playerColor == ChessGame.TeamColor.WHITE) ? ChessGame.TeamColor.BLACK : ChessGame.TeamColor.WHITE;
        GameStatus finalStatus = GameStatus.ACTIVE; // Assume active unless changed
        String endConditionNotificationText = null;

        if (currentGame.isInCheckmate(opponentColor)) {
            finalStatus = (opponentColor == ChessGame.TeamColor.BLACK) ? GameStatus.WHITE_WINS_CHECKMATE : GameStatus.BLACK_WINS_CHECKMATE;
            endConditionNotificationText = String.format("Checkmate! %s (%s) wins!", username, playerColor);
            System.out.println("Checkmate detected in game " + gameID);
        } else if (currentGame.isInStalemate(opponentColor)) {
            finalStatus = GameStatus.STALEMATE_DRAW;
            endConditionNotificationText = "Stalemate! The game is a draw.";
            System.out.println("Stalemate detected in game " + gameID);
        }

        // 5. Update database if makemove succeeded
        GameData updatedGameData = new GameData(
                gameData.gameID(),
                gameData.whiteUsername(),
                gameData.blackUsername(),
                gameData.gameName(),
                currentGame,
                gameData.status()
        );
        gameDAO.updateGame(updatedGameData); // Save the new state to DB

        // 6. Broadcast updated game state to clients
        LoadGameMessage loadGameMsg = new LoadGameMessage(currentGame); // Send the updated game
        String loadGameJson = gson.toJson(loadGameMsg);
        connectionManager.broadcast(gameID, null, loadGameJson); // Send to everyone
        System.out.println("Broadcast LOAD_GAME after move in game " + gameID);

        // 7. Broadcast move notification to other clients
        String moveNotation = move.toString();
        String notificationText = String.format("%s made move %s.", username, moveNotation);
        NotificationMessage notification = new NotificationMessage(notificationText);
        String notificationJson = gson.toJson(notification);
        connectionManager.broadcast(gameID, session, notificationJson); // Exclude player who moved
        System.out.println("Broadcast MOVE notification for game " + gameID);

        String checkNotificationText = null;
        if (finalStatus == GameStatus.ACTIVE && currentGame.isInCheck(opponentColor)) {
            checkNotificationText = String.format("%s is in Check!", opponentColor);
            System.out.println("Check detected in game " + gameID);
        }

        String finalNotificationText = (endConditionNotificationText != null) ? endConditionNotificationText : checkNotificationText;
        if (finalNotificationText != null) {
            NotificationMessage conditionNotification = new NotificationMessage(finalNotificationText);
            connectionManager.broadcast(gameID, null, gson.toJson(conditionNotification));
        }
    }

    private void handleLeave(Session session, String messageJson, String username) throws DataAccessException, IOException {
        System.out.println("Handling LEAVE for user: " + username);
        LeaveCommand leaveCmd = gson.fromJson(messageJson, LeaveCommand.class);
        Integer gameID = leaveCmd.getGameID();

        if (gameID == null) {
            sendError(session, "Error: Game ID missing in LEAVE command.");
            return;
        }

        GameData gameData = gameDAO.getGame(gameID);
        if (gameData != null) {
            GameData updatedGameData = gameData;
            boolean changed = false;
            if (username.equals(gameData.whiteUsername())) {
                updatedGameData = new GameData(gameID, null, gameData.blackUsername(), gameData.gameName(), gameData.game(), gameData.status());
                changed = true;
            } else if (username.equals(gameData.blackUsername())) {
                updatedGameData = new GameData(gameID, gameData.whiteUsername(), null, gameData.gameName(), gameData.game(), gameData.status());
                changed = true;
            }
            if (changed) {
                gameDAO.updateGame(updatedGameData);
                System.out.println("Updated game " + gameID + " - removed user " + username);
            }
        }

        // Remove session from connection manager
        connectionManager.remove(gameID, session);

        // Notify remaining clients
        String notificationText = String.format("%s left the game.", username);
        NotificationMessage notification = new NotificationMessage(notificationText);
        connectionManager.broadcast(gameID, session, gson.toJson(notification)); // Exclude self
        System.out.println("Broadcast LEAVE notification for game " + gameID);

    }

    private void handleResign(Session session, String messageJson, String username) throws DataAccessException, IOException {
        ResignCommand resignCmd = gson.fromJson(messageJson, ResignCommand.class);
        Integer gameID = resignCmd.getGameID();

        if (gameID == null) {
            sendError(session, "Error: Game ID missing in RESIGN command.");
            return;
        }

        // Get game, check if user is player
        GameData gameData = gameDAO.getGame(gameID);
        if (gameData == null) {
            sendError(session, "Error: Game " + gameID + " not found.");
            return;
        }

        if (gameData.status() != GameStatus.ACTIVE) {
            sendError(session, "Error: Cannot resign, game is already over (" + gameData.status() + ")."); return;
        }

        ChessGame.TeamColor resigningColor = null;
        if (username.equals(gameData.whiteUsername())) {
            resigningColor = ChessGame.TeamColor.WHITE;
        } else if (username.equals(gameData.blackUsername())) {
            resigningColor = ChessGame.TeamColor.BLACK;
        }
        if (resigningColor == null) {
            sendError(session, "Error: Observers cannot resign."); return;
        }

        GameStatus finalStatus = (resigningColor == ChessGame.TeamColor.WHITE) ? GameStatus.WHITE_RESIGNED : GameStatus.BLACK_RESIGNED;
        GameData updatedGameData = new GameData(
                gameData.gameID(), gameData.whiteUsername(), gameData.blackUsername(), gameData.gameName(),
                gameData.game(),
                finalStatus
        );
        gameDAO.updateGame(updatedGameData);

        // Notify clients (including resigning player)
        String winner = (resigningColor == ChessGame.TeamColor.WHITE) ? gameData.blackUsername() + " (BLACK)" : gameData.whiteUsername() + " (WHITE)";
        String notificationText = String.format("%s (%s) resigned. %s wins.", username, resigningColor, winner);
        NotificationMessage notification = new NotificationMessage(notificationText);
        connectionManager.broadcast(gameID, null, gson.toJson(notification)); // Send to everyone
    }

    // Helper to send an error message to a specific session
    private void sendError(Session session, String errorMessage) {
        try {
            if (session != null && session.isOpen()) {
                ErrorMessage errorMsg = new ErrorMessage(errorMessage);
                session.getRemote().sendString(gson.toJson(errorMsg));
            } else {
                System.err.println("Attempted to send error to closed/null session.");
            }
        } catch (Exception e) {
            System.err.println("Failed to send error message '" + errorMessage + "': " + e.getMessage());
        }
    }
}
