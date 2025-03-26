package ui;
import serverfacade.ServerFacade;
import exception.ResponseException;
import model.AuthData;
import model.GameData;
import service.results.ListGamesResult;
import service.results.ListGamesResult.GameInfo;

import java.util.Arrays;
import java.util.List;

public class ChessClient {
    private final ServerFacade server;
    private String authToken = null;
    private State state = State.LOGGEDOUT;

    public ChessClient(String serverUrl) {
        server = new ServerFacade(serverUrl);
    }

    
}


