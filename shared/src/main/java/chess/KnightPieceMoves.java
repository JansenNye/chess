package chess;
import java.util.Collection;
import static chess.ChessPiece.findOtherMoves;

public class KnightPieceMoves {

    private final Collection<ChessMove> knightMoves;

    public KnightPieceMoves(ChessPosition myPosition, ChessBoard myBoard) {
        int myRow = myPosition.getRow();
        int myCol = myPosition.getColumn();
        ChessPosition[] knightPositions = {
                new ChessPosition(myRow + 2, myCol + 1), //Up two and right one
                new ChessPosition(myRow + 2, myCol - 1), //Up two and left one
                new ChessPosition(myRow + 1, myCol + 2), //Up one and right two
                new ChessPosition(myRow + 1, myCol - 2), //Up one and left two
                new ChessPosition(myRow - 1, myCol + 2), //Down one and right two
                new ChessPosition(myRow - 1, myCol - 2), //Down one and left two
                new ChessPosition(myRow - 2, myCol + 1), //Down two and right one
                new ChessPosition(myRow - 2, myCol - 1)  //Down two and left one
        };
        this.knightMoves = findOtherMoves(myBoard, myPosition, knightPositions);
    }

    public Collection<ChessMove> getKnightMoves() {
        return this.knightMoves;
    }
}