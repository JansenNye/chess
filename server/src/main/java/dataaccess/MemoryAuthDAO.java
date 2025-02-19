package dataaccess;
import model.AuthData;
import java.util.HashMap;
import java.util.Map;

public class MemoryAuthDAO implements AuthDAO {
    private final Map<String, AuthData> authMap = new HashMap<>();

    @Override
    public void clear() throws DataAccessException {
        authMap.clear();
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        if (auth == null) {
            throw new DataAccessException("AuthData cannot be null");
        }
        if (authMap.containsKey(auth.authToken())) {
            throw new DataAccessException("Auth token already exists: " + auth.authToken());
        }
        authMap.put(auth.authToken(), auth);
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        return authMap.get(authToken);
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        authMap.remove(authToken);
    }
}
