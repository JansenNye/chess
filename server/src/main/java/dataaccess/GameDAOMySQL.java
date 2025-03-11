package dataaccess;

import model.GameData;

import com.google.gson.Gson;
import chess.ChessGame;

import java.sql.SQLException;
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
        // We are now manually inserting 'game_id'.
        // Ensure your DB schema doesn't have AUTO_INCREMENT on game_id.
        String gameJson = gson.toJson(game.game()); // Convert ChessGame to JSON

        String sql = "INSERT INTO games (game_id, white_username, black_username, game_name, game_state) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            // Insert the exact game_id from GameData
            stmt.setInt(1, game.gameID());
            stmt.setString(2, game.whiteUsername());
            stmt.setString(3, game.blackUsername());
            stmt.setString(4, game.gameName());
            stmt.setString(5, gameJson);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Error creating game");
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        String sql = "SELECT game_id, white_username, black_username, game_name, game_state "
                + "FROM games WHERE game_id = ?";

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, gameID);

            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String gameJson = rs.getString("game_state");
                    ChessGame chessGame = gson.fromJson(gameJson, ChessGame.class);

                    return new GameData(
                            rs.getInt("game_id"),
                            rs.getString("white_username"),
                            rs.getString("black_username"),
                            rs.getString("game_name"),
                            chessGame
                    );
                }
            } return null; // or throw an exception if no row found

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
        String sql = "UPDATE games "
                + "SET white_username = ?, black_username = ?, game_name = ?, game_state = ? "
                + "WHERE game_id = ?";

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