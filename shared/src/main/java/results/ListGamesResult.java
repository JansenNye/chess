package results;
import java.util.List;

public record ListGamesResult(List<GameInfo> games) {
    // Nested record w partial game info (not entire ChessGame)
    public record GameInfo(
            int gameID,
            String whiteUsername,
            String blackUsername,
            String gameName
    ) {}
}
