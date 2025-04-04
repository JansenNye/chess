package dataaccess;

import model.GameData;
import java.util.List;

public interface GameDAO {
    /**
     * CLEAR endpoint - removes all user data
     */
    void clear() throws DataAccessException;

    /**
     * Create a new game
     */
    void createGame(GameData game) throws DataAccessException;

    /**
     * Retrieve a game by gameID
     */
    GameData getGame(int gameID) throws DataAccessException;

    /**
     * Return all games
     */
    List<GameData> listGames() throws DataAccessException;

    /**
     * Update an existing game’s state - after a move, after a user joins
     */
    void updateGame(GameData game) throws DataAccessException;

}
