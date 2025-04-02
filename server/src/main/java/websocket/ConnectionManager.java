package websocket;

import org.eclipse.jetty.websocket.api.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class ConnectionManager {
    public static class Connection {
        public final String username;
        public final Session session;

        public Connection(String username, Session session) {
            this.username = username;
            this.session = session;
        }
    }

    public final ConcurrentHashMap<Integer, List<Connection>> gameConnections = new ConcurrentHashMap<>();

    /** Adds a connection for a given game. */
    public void add(Integer gameID, Session session, String username) {
        Connection newConnection = new Connection(username, session);
        gameConnections.computeIfAbsent(gameID, k -> new ArrayList<>()).add(newConnection);
        System.out.printf("Connection added for user '%s' to game %d%n", username, gameID);
    }

    /** Removes a specific connection from a game. */
    public void remove(Integer gameID, Session session) {
        List<Connection> gameSessions = gameConnections.get(gameID);
        if (gameSessions != null) {
            Predicate<Connection> condition = conn -> conn.session.equals(session);
            boolean removed = gameSessions.removeIf(condition);
            if (removed) {
                System.out.printf("Connection removed for session in game %d%n", gameID);
            }
            if (gameSessions.isEmpty()) {
                gameConnections.remove(gameID);
                System.out.printf("Game %d removed from active connections (no players).%n", gameID);
            }
        }
    }

    /** Removes a session from ALL games it might be in. */
    public void removeSessionGlobally(Session session) {
        System.out.println("Attempting global removal for session: " + session.getRemoteAddress());
        // Iterate through all game entries
        gameConnections.forEach((gameID, connections) -> {
            // Use removeIf to safely remove the session if found
            Predicate<Connection> condition = conn -> conn.session.equals(session);
            boolean removed = connections.removeIf(condition);
            if (removed) {
                System.out.printf("Removed session %s from game %d during global cleanup%n", session.getRemoteAddress(), gameID);
                // Check if the list for this game is now empty
                if (connections.isEmpty()) {
                    gameConnections.remove(gameID);
                    System.out.printf("Game %d removed from active connections after global cleanup.%n", gameID);
                }
            }
        });
    }


    public void broadcast(Integer gameID, Session excludeSession, String messageJson) {
        List<Connection> gameConnectionsList = gameConnections.get(gameID);
        if (gameConnectionsList == null || gameConnectionsList.isEmpty()) {
            // System.out.println("No sessions for game " + gameID + " to broadcast.");
            return;
        }

        List<Connection> sessionsToRemove = new ArrayList<>(); // Store disconnected sessions

        // Iterate safely over the list (can't modify while iterating directly)
        for (Connection conn : new ArrayList<>(gameConnectionsList)) { // Iterate over a copy
            if (conn.session.isOpen()) {
                boolean shouldExclude = (excludeSession != null && conn.session.equals(excludeSession));
                if (!shouldExclude) {
                    try {
                        conn.session.getRemote().sendString(messageJson); // Send the JSON message
                        // System.out.printf("Broadcast message sent to '%s' in game %d%n", conn.username, gameID);
                    } catch (IOException e) {
                        System.err.printf("ERROR broadcasting to user '%s' in game %d: %s%n", conn.username, gameID, e.getMessage());

                        sessionsToRemove.add(conn);
                    }
                }
            } else {
                sessionsToRemove.add(conn);
            }
        }

        // Remove any sessions that failed or were closed
        if (!sessionsToRemove.isEmpty()) {
            List<Connection> currentList = gameConnections.get(gameID);
            if(currentList != null) {
                boolean changed = currentList.removeAll(sessionsToRemove);
                if (changed) {
                    System.out.printf("Removed %d stale/erroring connections from game %d%n", sessionsToRemove.size(), gameID);
                    if (currentList.isEmpty()) {
                        gameConnections.remove(gameID);
                        System.out.printf("Game %d removed (empty) after cleanup.%n", gameID);
                    }
                }
            }
        }
    }
}