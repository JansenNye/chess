package ui;
import websocket.messages.ServerMessage;
public interface ServerMessageObserver {
    /**
     * Called when a ServerMessage is received from the WebSocket connection
     * @param message The message received from the server
     */
    void notify(ServerMessage message);
}

