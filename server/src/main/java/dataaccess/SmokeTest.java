package dataaccess;

import model.UserData;

public class SmokeTest {
    public static void main(String[] args) {
        try {
            // We'll add our test steps here
            DatabaseManager.createDatabase();
            UserDAOMySQL userDao = new UserDAOMySQL();
            UserData newUser = new UserData(
                    "alice",       // username
                    "alicePass",   // password (hashed or plain—just for this test)
                    "alice@byu.edu" // email
            );
            userDao.createUser(newUser);
            System.out.println("Created user: " + newUser);
            UserData fetchedUser = userDao.getUser("alice");
            if (fetchedUser != null) {
                System.out.println("Retrieved user: " + fetchedUser);
            } else {
                System.out.println("User not found!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}