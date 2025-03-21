import chess.*;

public class ServerFacade {
    // Example of a stubbed login method
    public boolean login(String username, String password) {
        // TODO: Replace with real HTTP call to your server's login endpoint
        // For now, let's assume any non-empty username/password is successful
        return !username.isEmpty() && !password.isEmpty();
    }
}
