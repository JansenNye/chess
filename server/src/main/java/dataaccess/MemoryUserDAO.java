package dataaccess;

import model.UserData;
import java.util.HashMap;
import java.util.Map;

public class MemoryUserDAO implements UserDAO {
    private final Map<String, UserData> users = new HashMap<>();

    @Override
    public void clear() throws DataAccessException {
        users.clear();
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        if (user == null) {
            throw new DataAccessException("User cannot be null");
        } String username = user.username();
        if (users.containsKey(username)) {
            throw new DataAccessException("User already exists: " + username);
        } users.put(username, user);
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        if (username == null || !users.containsKey(username)) {
            return null;
        } return users.get(username);
    }
}
