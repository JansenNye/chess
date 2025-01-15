package chess;

import java.util.Collection;

import static chess.ChessPiece.findDistanceMoves;

public class QueenPieceMoves {

    private final Collection<ChessMove> QueenMoves;

    public QueenPieceMoves(ChessPosition myPosition, ChessBoard myBoard) {
        int[][] queenDirections = {
                {1, 1},   // Up and right
                {1, 0},   // Up
                {1, -1},  // Up and left
                {0, 1},   // Right
                {0, -1},  // Left
                {-1, 1},  // Down and right
                {-1, 0},  // Down
                {-1, -1}, // Down and left
        };
        this.QueenMoves = findDistanceMoves(myBoard, myPosition, queenDirections);
    }

    public Collection<ChessMove> getQueenMoves() {
        return this.QueenMoves;
    }
}
