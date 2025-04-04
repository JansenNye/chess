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

    /**
     * Adds a connection for a given game.
     */
    public void add(Integer gameID, Session session, String username) {
        Connection newConnection = new Connection(username, session);
        gameConnections.computeIfAbsent(gameID, k -> new ArrayList<>()).add(newConnection);
    }

    /**
     * Removes a specific connection from a game.
     */
    public void remove(Integer gameID, Session session) {
        List<Connection> gameSessions = gameConnections.get(gameID);
        if (gameSessions != null) {
            Predicate<Connection> condition = conn -> conn.session.equals(session);
            boolean removed = gameSessions.removeIf(condition);
            if (gameSessions.isEmpty()) {
                gameConnections.remove(gameID);
            }
        }
    }

    /**
     * Removes a session from ALL games it might be in.
     */
    public void removeSessionGlobally(Session session) {
        // Iterate through all game entries
        gameConnections.forEach((gameID, connections) -> {
            // Use removeIf to safely remove the session if found
            Predicate<Connection> condition = conn -> conn.session.equals(session);
            boolean removed = connections.removeIf(condition);
            if (removed) {
                if (connections.isEmpty()) {
                    gameConnections.remove(gameID);
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
            if (currentList != null) {
                boolean changed = currentList.removeAll(sessionsToRemove);
                if (changed) {
                    if (currentList.isEmpty()) {
                        gameConnections.remove(gameID);
                    }
                }
            }
        }
    }
}