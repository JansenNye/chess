package websocket;

import com.google.gson.Gson;
import ui.ServerMessageObserver;
import websocket.messages.ServerMessage;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@ClientEndpoint
public class WebSocketCommunicator {

    private Session session;
    private ServerMessageObserver observer; // Observer to notify
    private Gson gson = new Gson();

    // Constructor
    public WebSocketCommunicator(String serverUri, ServerMessageObserver observer) throws DeploymentException, IOException, URISyntaxException {
        this.observer = observer;
        URI uri = new URI(serverUri + "/ws");
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        // Attempt to connect and establish the session, pass 'this' instance to connectToServer
        container.connectToServer(this, uri);
    }

    // Called when the WebSocket connection is successfully opened
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        //Set timeout to 10 minutes instead of 5 - could be longer?
        session.setMaxIdleTimeout(600000);
        // System.out.println("WebSocket connection opened.");
    }

    // Called when a message is received from the server
    @OnMessage
    public void onMessage(String messageJson) {
        try {
            // Deserialize the message using Gson
            ServerMessage baseMessage = gson.fromJson(messageJson, ServerMessage.class);

            ServerMessage specificMessage = switch (baseMessage.getServerMessageType()) {
                case LOAD_GAME -> gson.fromJson(messageJson, websocket.messages.LoadGameMessage.class);
                case ERROR -> gson.fromJson(messageJson, websocket.messages.ErrorMessage.class);
                case NOTIFICATION -> gson.fromJson(messageJson, websocket.messages.NotificationMessage.class);
                // Any other messages ?
            };

            // Notify observer with the specific message object
            if (observer != null) {
                observer.notify(specificMessage);
            } else {
                System.out.println("Observer is null, cannot notify.");
            }

        } catch (Exception e) {
            System.err.println("Error processing server message." + messageJson);
            System.err.println("Exception: " + e.getMessage());
            e.printStackTrace();
            // notify observer?
            if (observer != null) {
                observer.notify(new websocket.messages.ErrorMessage("Failed to parse server message: " + e.getMessage()));
            }
        }
    }

    // Called when the WebSocket connection is closed
    @OnClose
    public void onClose(Session session, CloseReason reason) {
        this.session = null; // Mark session as closed
        // System.out.println("WebSocket connection closed" + reason.getReasonPhrase());
    }

    // Called when a WebSocket error occurs
    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket error: " + throwable.getMessage());
        throwable.printStackTrace();
        if (observer != null) {
            observer.notify(new websocket.messages.ErrorMessage("WebSocket communication error: " + throwable.getMessage()));
        }
    }

    // Send message (UserGameCommand) to the server
    public void sendMessage(Object message) throws IOException {
        if (this.session != null && this.session.isOpen()) {
            String messageJson = gson.toJson(message);
            this.session.getBasicRemote().sendText(messageJson);
        } else {
            System.err.println("Cannot send message: WebSocket session is not open.");
            // Maybe throw exception or handle differently?
            throw new IOException("WebSocket session is not open.");
        }
    }

    // Explicitly close WebSocket connection
    public void close() throws IOException {
        if (this.session != null && this.session.isOpen()) {
            this.session.close();
            this.session = null; // Ensure session is null after closing
            // System.out.println("WebSocket explicitly closed by client.");
        }
    }

    // Check if connection is open
    public boolean isOpen() {
        return this.session != null && this.session.isOpen();
    }
}