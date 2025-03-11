package dataaccess;

import model.AuthData;
import model.UserData;

public class DAOTest {
    public static void main(String[] args) {
        try {
            // Create or ensure 'chess' database exists
            DatabaseManager.createDatabase();

            var userDao = new UserDAOMySQL();
            var authDao = new AuthDAOMySQL();

            userDao.clear();
            authDao.clear();
            System.out.println("Cleared both tables.");

            var alice = new UserData("alice", "alicePass", "alice@byu.edu");
            userDao.createUser(alice);
            System.out.println("Created user: " + userDao.getUser("alice"));

            var auth = new AuthData("abc123", "alice");
            authDao.createAuth(auth);
            System.out.println("Created auth token: " + authDao.getAuth("abc123"));

            userDao.clear();
            var checkAuth = authDao.getAuth("abc123");
            System.out.println("After clearing users, auth token 'abc123' is: " + checkAuth);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}