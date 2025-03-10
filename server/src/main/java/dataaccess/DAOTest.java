package dataaccess;

import model.AuthData;
import model.UserData;

public class DAOTest {
    public static void main(String[] args) {
        try {
            // 1. Create or ensure the 'chess' database exists
            DatabaseManager.createDatabase();

            // (Optional) 2. If you want to run CREATE TABLE statements here with ON DELETE CASCADE,
            // you can do so. Otherwise, do it once in MySQL Workbench or a startup script.

            // 3. Instantiate DAOs
            var userDao = new UserDAOMySQL();
            var authDao = new AuthDAOMySQL();

            // 4. Clear both tables (order doesn't matter with ON DELETE CASCADE)
            userDao.clear();
            authDao.clear();
            System.out.println("Cleared both tables.");

            // 5. Insert a user
            var alice = new UserData("alice", "alicePass", "alice@byu.edu");
            userDao.createUser(alice);
            System.out.println("Created user: " + userDao.getUser("alice"));

            // 6. Insert an auth token referencing the user
            var auth = new AuthData("abc123", "alice");
            authDao.createAuth(auth);
            System.out.println("Created auth token: " + authDao.getAuth("abc123"));

            // 7. (Optional) Demonstrate cascade by deleting the user and verifying the auth is also gone
            userDao.clear(); // This deletes all users
            // Because of ON DELETE CASCADE, all auth rows for those users are automatically removed
            var checkAuth = authDao.getAuth("abc123");
            System.out.println("After clearing users, auth token 'abc123' is: " + checkAuth);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}