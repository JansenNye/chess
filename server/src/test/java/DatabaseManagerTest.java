import dataaccess.DatabaseManager;
import org.junit.jupiter.api.Test;

public class DatabaseManagerTest {

    @Test
    public void testConnectionTest() throws Exception {
        DatabaseManager dbm = new DatabaseManager();
        dbm.testConnection();
    }
}