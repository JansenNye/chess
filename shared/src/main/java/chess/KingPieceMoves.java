package chess;
import java.util.Collection;
import static chess.ChessPiece.findOtherMoves;

public class KingPieceMoves {

    private final Collection<ChessMove> kingMoves;

    public KingPieceMoves(ChessPosition myPosition, ChessBoard myBoard) {
        int myRow = myPosition.getRow();
        int myCol = myPosition.getColumn();
        ChessPosition[] kingPositions = {
                new ChessPosition(myRow + 1, myCol + 1), //Up and right
                new ChessPosition(myRow + 1, myCol), //Up
                new ChessPosition(myRow + 1, myCol - 1), //Up and left
                new ChessPosition(myRow, myCol + 1), //Right
                new ChessPosition(myRow, myCol - 1), //Left
                new ChessPosition(myRow - 1, myCol + 1), //Down and right
                new ChessPosition(myRow - 1, myCol), //Down
                new ChessPosition(myRow - 1, myCol - 1) //Down and left
        };
        Collection<ChessMove> kingMoves = findOtherMoves(myBoard, myPosition, kingPositions);
        this.kingMoves = addCastlingMoves(myBoard, myPosition, kingMoves);
    }

    private Collection<ChessMove> addCastlingMoves(ChessBoard board, ChessPosition position, Collection<ChessMove> kingMoves) {
        int myRow = position.getRow();
        int myCol = position.getColumn();
        if (!board.getPiece(position).hasMoved) {
            if (myCol + 3 <= 8 && board.getPiece(new ChessPosition(myRow, myCol + 1)) == null && //King-side
                    board.getPiece(new ChessPosition(myRow, myCol + 2)) == null) {
                ChessPiece kingRook = board.getPiece(new ChessPosition(myRow, myCol + 3));
                if (kingRook != null && !kingRook.hasMoved) {
                    kingMoves.add(new ChessMove(position, new ChessPosition(myRow, myCol + 2), null));
                }
            } if (myCol - 4 >= 1 && board.getPiece(new ChessPosition(myRow, myCol - 1)) == null && //Queen-side
                    board.getPiece(new ChessPosition(myRow, myCol - 2)) == null) {
                ChessPiece queenRook = board.getPiece(new ChessPosition(myRow, myCol - 4));
                if (queenRook != null && !queenRook.hasMoved) {
                    kingMoves.add(new ChessMove(position, new ChessPosition(myRow, myCol - 2), null));
                }
            }
        } return kingMoves;
    }
    public Collection<ChessMove> getKingMoves(){
        return this.kingMoves;
    }
}
