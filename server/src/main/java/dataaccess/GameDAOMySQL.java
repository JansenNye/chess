package dataaccess;
import model.GameData;
import com.google.gson.Gson;
import chess.ChessGame;
import model.GameStatus;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GameDAOMySQL implements GameDAO {

    private final Gson gson = new Gson(); // serialize/deserialize ChessGame

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
        String gameJson = gson.toJson(game.game());

        String sql = "INSERT INTO games (game_id, white_username, black_username, game_name, game_state, status ) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            // Insert game_id from GameData
            stmt.setInt(1, game.gameID());
            stmt.setString(2, game.whiteUsername());
            stmt.setString(3, game.blackUsername());
            stmt.setString(4, game.gameName());
            stmt.setString(5, gameJson);
            stmt.setString(6, game.status().name());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Error creating game");
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        String sql = "SELECT game_id, white_username, black_username, game_name, game_state, status "
                + "FROM games WHERE game_id = ?";

        try (var conn = DatabaseManager.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, gameID);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String gameJson = rs.getString("game_state");
                    ChessGame chessGame = gson.fromJson(gameJson, ChessGame.class);
                    String statusStr = rs.getString("status"); // Get status string
                    GameStatus status = GameStatus.valueOf(statusStr); // Convert string back to enum

                    return new GameData(
                            rs.getInt("game_id"),
                            rs.getString("white_username"),
                            rs.getString("black_username"),
                            rs.getString("game_name"),
                            chessGame,
                            status
                    );
                }
            } return null;

        } catch (SQLException e) {
            throw new DataAccessException("Error retrieving game");
        }
    }

    @Override
    public List<GameData> listGames() throws DataAccessException {
        String sql = "SELECT game_id, white_username, black_username, game_name, game_state, status FROM games";
        List<GameData> games = new ArrayList<>();

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {
            while (rs.next()) {
                String gameJson = rs.getString("game_state");
                ChessGame chessGame = gson.fromJson(gameJson, ChessGame.class);
                String statusStr = rs.getString("status");
                GameStatus status = GameStatus.valueOf(statusStr);
                GameData gameData = new GameData(
                        rs.getInt("game_id"),
                        rs.getString("white_username"),
                        rs.getString("black_username"),
                        rs.getString("game_name"),
                        chessGame,
                        status
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
                + "SET white_username = ?, black_username = ?, game_name = ?, game_state = ?, status = ? "
                + "WHERE game_id = ?";

        String gameJson = gson.toJson(game.game());

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, game.whiteUsername());
            stmt.setString(2, game.blackUsername());
            stmt.setString(3, game.gameName());
            stmt.setString(4, gameJson);
            stmt.setString(5, game.status().name());
            stmt.setInt(6, game.gameID());
            //is this correct?

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Error updating game");
        }
    }

    @Override
    // UNUSED
    public void deleteGame(int gameID, GameData game) throws DataAccessException {
        String sql = "DELETE FROM games WHERE game_id = ?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, gameID);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                System.out.println("Warning: Attempted to delete non-existent game ID: " + gameID);
            } else {
                System.out.println("Deleted game: " + game.gameName());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error deleting game");
        }
    }
}