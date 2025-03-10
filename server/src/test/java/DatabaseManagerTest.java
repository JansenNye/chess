import dataaccess.DatabaseManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DatabaseManagerTest {

    @Test
    public void testConnectionTest() throws Exception {
        DatabaseManager dbm = new DatabaseManager();
        dbm.testConnection(); // If this doesn’t throw, we assume success
        // You could add assertions or checks here if you’d like.
    }
}