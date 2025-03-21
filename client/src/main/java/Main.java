import chess.*;

import java.util.Scanner;

public class Main {

    // Track login state
    private static boolean isLoggedIn = false;

    public static void main(String[] args) {
        var piece = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);
        System.out.println("♕ 240 Chess Client: " + piece);

        // Start in prelogin mode
        preloginLoop();
    }

    private static void preloginLoop() {
        Scanner scanner = new Scanner(System.in);
        ServerFacade facade = new ServerFacade();

        System.out.println("Welcome to Chess. Type 'help' to get started, or 'exit' to quit.");

        // Loop until user logs in or exits
        while (!isLoggedIn) {
            System.out.print("> ");
            String input = scanner.nextLine().trim().toLowerCase();

            switch (input) {
                case "help":
                    printPreloginHelp();
                    break;

                case "login":
                    handleLogin(scanner, facade);
                    break;

                case "exit":
                    System.out.println("Exiting the program...");
                    System.exit(0);

                default:
                    System.out.println("Unrecognized command. Type 'help' for a list of commands.");
                    break;
            }
        }

        // Once logged in, move on to postlogin
        postloginLoop(scanner, facade);
    }

    /**
     * Prompt for username and password, call the facade, handle success/failure.
     */
    private static void handleLogin(Scanner scanner, ServerFacade facade) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        // Call ServerFacade
        boolean success = facade.login(username, password);

        if (success) {
            System.out.println("Login successful!");
            isLoggedIn = true;  // Break out of prelogin loop
        } else {
            System.out.println("Login failed. Please try again.");
        }
    }

    private static void postloginLoop(Scanner scanner, ServerFacade facade) {
        System.out.println("You are now logged in. Type 'help' to see postlogin commands, or 'logout' to log out.");

        while (isLoggedIn) {
            System.out.print("(postlogin) > ");
            String input = scanner.nextLine().trim().toLowerCase();

            switch (input) {
                case "help":
                    printPostloginHelp();
                    break;

                case "logout":
                    System.out.println("Logging out...");
                    isLoggedIn = false;
                    // After logging out, go back to prelogin loop
                    preloginLoop();
                    return; // Don’t keep processing postlogin commands

                case "exit":
                    System.out.println("Exiting the program...");
                    System.exit(0);

                default:
                    System.out.println("Unrecognized command (postlogin). Type 'help' for a list of commands.");
                    break;
            }
        }
    }

    private static void printPreloginHelp() {
        System.out.println("\n=== Prelogin Commands ===");
        System.out.println("help       - Show this help text");
        System.out.println("login      - Login to your account");
        System.out.println("exit       - Quit the program");
        System.out.println("=========================\n");
    }

    private static void printPostloginHelp() {
        System.out.println("\n=== Postlogin Commands ===");
        System.out.println("help       - Show this help text");
        System.out.println("logout     - Logout of your account");
        System.out.println("exit       - Quit the program");
        System.out.println("==========================\n");
    }
}