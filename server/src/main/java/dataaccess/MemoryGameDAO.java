package dataaccess;
import model.GameData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemoryGameDAO implements GameDAO {
    private final Map<Integer, GameData> games = new HashMap<>();

    @Override
    public void clear() throws DataAccessException {
        games.clear();
    }

    @Override
    public void createGame(GameData game) throws DataAccessException {
        if (game == null) {
            throw new DataAccessException("Game cannot be null");
        }
        int gameID = game.gameID();
        if (games.containsKey(gameID)) {
            throw new DataAccessException("Game already exists with ID: " + gameID);
        }
        games.put(gameID, game);
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        return games.get(gameID); // returns null if not found
    }

    @Override
    public List<GameData> listGames() throws DataAccessException {
        return new ArrayList<>(games.values());
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        if (game == null) {
            throw new DataAccessException("Game cannot be null");
        }
        int gameID = game.gameID();
        if (!games.containsKey(gameID)) {
            throw new DataAccessException("Game not found with ID: " + gameID);
        }
        games.put(gameID, game);
    }

    @Override
    // UNUSED
    public void deleteGame(int gameID, GameData game) throws DataAccessException {
        if (game == null) {
            throw new DataAccessException("Game cannot be null");
        } else {
            games.remove(gameID);
        }
    }
}
