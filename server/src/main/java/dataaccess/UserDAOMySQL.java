package dataaccess;
import model.UserData;
import java.sql.SQLException;
public class UserDAOMySQL implements UserDAO {

    @Override
    public void clear() throws DataAccessException {
        String sql = "DELETE FROM users";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error clearing users table");
        }
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {

        String hashedPassword = user.password();
        String sql = "INSERT INTO users (username, hashed_password, email) VALUES (?, ?, ?)";

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.username());
            stmt.setString(2, hashedPassword);
            stmt.setString(3, user.email());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error creating user");
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        String sql = "SELECT username, hashed_password, email FROM users WHERE username = ?";

        try (var conn = DatabaseManager.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Construct UserData record from row
                    return new UserData(
                            rs.getString("username"),
                            rs.getString("hashed_password"),
                            rs.getString("email")
                    );
                }
            } return null;
        } catch (SQLException e) {
            throw new DataAccessException("Error retrieving user");
        }
    }
}