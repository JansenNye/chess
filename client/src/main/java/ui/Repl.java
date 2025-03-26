package ui;

import java.util.Scanner;
import static ui.EscapeSequences.*;

public class Repl {
    private final ChessClient client;
    public Repl(String serverUrl) {
        client = new ChessClient(serverUrl);
    }
    public void run() {

        System.out.println(SET_TEXT_COLOR_WHITE + "Welcome!");
        System.out.print(SET_TEXT_COLOR_BLUE + client.help());
        Scanner scanner = new Scanner(System.in);
        var result = "";

        //print
        while(!result.equals("quit")) {
            printPrompt();

            String line = scanner.nextLine();

            try
            {
                result = client.eval(line);
                System.out.println(SET_TEXT_COLOR_BLUE + result);
            }
            catch (Throwable e)
                    
            {
                var msg = e.getMessage();
                System.out.print(msg);
            }
        }
        System.out.println();
    }

    private void printPrompt() {
        System.out.print("\n" + RESET_TEXT_COLOR +  ">>> " + SET_TEXT_COLOR_GREEN);
    }

}

