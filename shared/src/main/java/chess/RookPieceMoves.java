package chess;

import java.util.Collection;

import static chess.ChessPiece.findDistanceMoves;

public class RookPieceMoves {

    private final Collection<ChessMove> RookMoves;

    public RookPieceMoves(ChessPosition myPosition, ChessBoard myBoard) {
        int[][] rookDirections = {
                {1, 0},   // Up
                {0, 1},   // Right
                {0, -1},  // Left
                {-1, 0},  // Down
        };
        this.RookMoves = findDistanceMoves(myBoard, myPosition, rookDirections);
    }

    public Collection<ChessMove> getRookMoves() {
        return this.RookMoves;
    }
}
