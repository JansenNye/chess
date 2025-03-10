package dataaccess;

import model.GameData;

import com.google.gson.Gson;
import chess.ChessGame;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GameDAOMySQL implements GameDAO {

    private final Gson gson = new Gson(); // We'll use this to serialize/deserialize ChessGame

    @Override
    public void clear() throws DataAccessException {
        String sql = "DELETE FROM games";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error clearing games table");
        }
    }

    @Override
    public void createGame(GameData game) throws DataAccessException {
        // We'll assume 'game_id' is auto-increment in the DB, so we won't insert it directly.
        // We'll store the ChessGame object in a 'game_state' column as JSON.
        String gameJson = gson.toJson(game.game()); // Convert ChessGame to JSON

        String sql = "INSERT INTO games (white_username, black_username, game_name, game_state) "
                + "VALUES (?, ?, ?, ?)";

        try (var conn = DatabaseManager.getConnection();
             // Statement.RETURN_GENERATED_KEYS lets us retrieve the auto-generated game_id if we want it
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, game.whiteUsername());
            stmt.setString(2, game.blackUsername());
            stmt.setString(3, game.gameName());
            stmt.setString(4, gameJson);

            stmt.executeUpdate();

            // If you want to capture the new auto-generated ID from the DB:
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int newId = rs.getInt(1);
                    // Optionally, you could log this or store it somewhere.
                    // For example, you could create a new GameData with the newId:
                    // GameData newGame = new GameData(newId, game.whiteUsername(), game.blackUsername(), game.gameName(), game.game());
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Error creating game");
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        // We'll fetch columns from the 'games' table and deserialize 'game_state' back into a ChessGame
        String sql = "SELECT game_id, white_username, black_username, game_name, game_state "
                + "FROM games WHERE game_id = ?";

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, gameID);

            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Deserialize the JSON from 'game_state' back into a ChessGame
                    String gameJson = rs.getString("game_state");
                    ChessGame chessGame = gson.fromJson(gameJson, ChessGame.class);

                    // Construct and return a new GameData record
                    return new GameData(
                            rs.getInt("game_id"),
                            rs.getString("white_username"),
                            rs.getString("black_username"),
                            rs.getString("game_name"),
                            chessGame
                    );
                }
            }
            // If no rows found, return null or throw an exception—your choice
            return null;

        } catch (SQLException e) {
            throw new DataAccessException("Error retrieving game");
        }
    }

    @Override
    public List<GameData> listGames() throws DataAccessException {
        String sql = "SELECT game_id, white_username, black_username, game_name, game_state FROM games";
        List<GameData> games = new ArrayList<>();

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {

            while (rs.next()) {
                String gameJson = rs.getString("game_state");
                ChessGame chessGame = gson.fromJson(gameJson, ChessGame.class);

                GameData gameData = new GameData(
                        rs.getInt("game_id"),
                        rs.getString("white_username"),
                        rs.getString("black_username"),
                        rs.getString("game_name"),
                        chessGame
                );
                games.add(gameData);
            }
            return games;

        } catch (SQLException e) {
            throw new DataAccessException("Error listing games");
        }
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        // We'll assume 'game.gameID()' is the primary key to identify which row to update.
        String sql = "UPDATE games "
                + "SET white_username = ?, black_username = ?, game_name = ?, game_state = ? "
                + "WHERE game_id = ?";

        // Serialize the updated ChessGame object
        String gameJson = gson.toJson(game.game());

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, game.whiteUsername());
            stmt.setString(2, game.blackUsername());
            stmt.setString(3, game.gameName());
            stmt.setString(4, gameJson);
            stmt.setInt(5, game.gameID());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Error updating game");
        }
    }
}