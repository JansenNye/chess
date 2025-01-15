package chess;

import java.util.ArrayList;
import java.util.Collection;

public class PawnPieceMoves {
    private final Collection<ChessMove> pawnMoves = new ArrayList<>();

    public PawnPieceMoves(ChessPosition myPosition, ChessBoard myBoard) {
        int myRow = myPosition.getRow();
        int myCol = myPosition.getColumn();
        if (myBoard.getPiece(myPosition).getTeamColor() == ChessGame.TeamColor.BLACK) { // ! Black pawns - move down board
            ChessPosition BlackPawnOneSquare = new ChessPosition(myRow - 1, myCol); //Standard move
            ChessPosition BlackPawnTakeLeft = new ChessPosition(myRow - 1, myCol - 1); //Diagonal capture
            ChessPosition BlackPawnTakeRight = new ChessPosition(myRow - 1, myCol + 1); //Diagonal capture
            if (myRow != 2) { // * Pawn is not promoting
                if (myBoard.getPiece(BlackPawnOneSquare) == null) { // * One square
                    pawnMoves.add(new ChessMove(myPosition, BlackPawnOneSquare, null));
                    if (myRow == 7) { // * First pawn move
                        ChessPosition BlackPawnTwoSquares = new ChessPosition(myRow - 2, myCol); // * Two-square move
                        if (myBoard.getPiece(BlackPawnTwoSquares) == null) { // * Move is available
                            pawnMoves.add(new ChessMove(myPosition, BlackPawnTwoSquares, null));
                        }
                    }
                } if (myCol > 1) { // ? Capture left
                    if (myBoard.getPiece(BlackPawnTakeLeft) != null) {
                        if (myBoard.getPiece(BlackPawnTakeLeft).getTeamColor() == ChessGame.TeamColor.WHITE) {
                            pawnMoves.add(new ChessMove(myPosition, BlackPawnTakeLeft, null));
                        }
                    }
                } if (myCol < 8) { // ? Capture right
                    if (myBoard.getPiece(BlackPawnTakeRight) != null) {
                        if (myBoard.getPiece(BlackPawnTakeRight).getTeamColor() == ChessGame.TeamColor.WHITE) {
                            pawnMoves.add(new ChessMove(myPosition, BlackPawnTakeRight, null));
                        }
                    }
                }
            } else { // ~ Pawn is promoting
                pawnPromote(myBoard, myPosition, pawnMoves, BlackPawnOneSquare, BlackPawnTakeLeft, BlackPawnTakeRight);
            }
        } else { // ! White pawns - move up board
            ChessPosition WhitePawnOneSquare = new ChessPosition(myRow + 1, myCol); //Standard move
            ChessPosition WhitePawnTakeLeft = new ChessPosition(myRow + 1, myCol - 1); //Diagonal capture
            ChessPosition WhitePawnTakeRight = new ChessPosition(myRow + 1, myCol + 1); //Diagonal capture
            if (myRow != 7) { // * Pawn is not promoting
                if (myBoard.getPiece(WhitePawnOneSquare) == null) { // * One square
                    pawnMoves.add(new ChessMove(myPosition, WhitePawnOneSquare, null));
                    if (myRow == 2) { // * First pawn move
                        ChessPosition WhitePawnTwoSquares = new ChessPosition(myRow + 2, myCol); // * Two-square move
                        if (myBoard.getPiece(WhitePawnTwoSquares) == null) { // * Move is available
                            pawnMoves.add(new ChessMove(myPosition, WhitePawnTwoSquares, null));
                        }
                    }
                }
                if (myCol > 1) { // ? Capture left
                    if (myBoard.getPiece(WhitePawnTakeLeft) != null) {
                        if (myBoard.getPiece(WhitePawnTakeLeft).getTeamColor() == ChessGame.TeamColor.BLACK) {
                            pawnMoves.add(new ChessMove(myPosition, WhitePawnTakeLeft, null));
                        }
                    }
                }
                if (myCol < 8) { // ? Capture right
                    if (myBoard.getPiece(WhitePawnTakeRight) != null) {
                        if (myBoard.getPiece(WhitePawnTakeRight).getTeamColor() == ChessGame.TeamColor.BLACK) {
                            pawnMoves.add(new ChessMove(myPosition, WhitePawnTakeRight, null));
                        }
                    }
                }
            } else { // ~ Pawn is promoting
                pawnPromote(myBoard, myPosition, pawnMoves, WhitePawnOneSquare, WhitePawnTakeLeft, WhitePawnTakeRight);
            }
        }
    }
    public void pawnPromote(ChessBoard board, ChessPosition myPosition, Collection<ChessMove> pawnMoves, ChessPosition PawnOneSquare, ChessPosition PawnTakeLeft, ChessPosition PawnTakeRight) {
        addAllPromotions(myPosition, pawnMoves, PawnOneSquare);
        if(myPosition.getColumn() > 1) {
            pawnPromotionCapture(board, myPosition, pawnMoves, PawnTakeLeft);
        } if(myPosition.getColumn() < 8) {
            pawnPromotionCapture(board, myPosition, pawnMoves, PawnTakeRight);
        }
    }

    //Helper function for pawnPromote handling pawn promotion captures
    public void pawnPromotionCapture(ChessBoard board, ChessPosition myPosition, Collection<ChessMove> pawnMoves, ChessPosition pawnTake) {
        if (board.getPiece(pawnTake) != null) {
            if (board.getPiece(pawnTake).getTeamColor() != board.getPiece(myPosition).getTeamColor()) {
                addAllPromotions(myPosition, pawnMoves, pawnTake);
            }
        }
    }

    private void addAllPromotions(ChessPosition myPosition, Collection<ChessMove> pawnMoves, ChessPosition pawnTake) {
        pawnMoves.add(new ChessMove(myPosition, pawnTake, ChessPiece.PieceType.QUEEN));
        pawnMoves.add(new ChessMove(myPosition, pawnTake, ChessPiece.PieceType.KNIGHT));
        pawnMoves.add(new ChessMove(myPosition, pawnTake, ChessPiece.PieceType.ROOK));
        pawnMoves.add(new ChessMove(myPosition, pawnTake, ChessPiece.PieceType.BISHOP));
    }

    public Collection<ChessMove> getPawnMoves() {
        return this.pawnMoves;
    }
}
