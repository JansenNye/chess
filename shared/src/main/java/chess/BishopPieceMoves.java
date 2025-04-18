package chess;

import java.util.Collection;

import static chess.ChessPiece.findDistanceMoves;

public class BishopPieceMoves {

    private final Collection<ChessMove> bishopMoves;

    public BishopPieceMoves(ChessPosition myPosition, ChessBoard myBoard) {
        int[][] bishopDirections = {
                {1, 1},   // Up and right
                {1, -1},  // Up and left
                {-1, 1},  // Down and right
                {-1, -1}, // Down and left
        };
        this.bishopMoves = findDistanceMoves(myBoard, myPosition, bishopDirections);
    }

    public Collection<ChessMove> getBishopMoves() {
        return this.bishopMoves;
    }
}
