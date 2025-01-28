package chess;

import java.util.ArrayList;
import java.util.Collection;

public class PawnPieceMoves {
    private final Collection<ChessMove> pawnMoves = new ArrayList<>();

    public PawnPieceMoves(ChessPosition myPosition, ChessBoard myBoard) {
        int myRow = myPosition.getRow();
        int myCol = myPosition.getColumn();
        ChessGame.TeamColor teamColor = myBoard.getPiece(myPosition).getTeamColor();

        int direction = (teamColor == ChessGame.TeamColor.BLACK) ? -1 : 1;
        int startRow = (teamColor == ChessGame.TeamColor.BLACK) ? 7 : 2;
        int promotionRow = (teamColor == ChessGame.TeamColor.BLACK) ? 2 : 7;
        ChessGame.TeamColor opponentColor = (teamColor == ChessGame.TeamColor.BLACK) ? ChessGame.TeamColor.WHITE : ChessGame.TeamColor.BLACK;

        ChessPosition oneSquareMove = new ChessPosition(myRow + direction, myCol);
        ChessPosition twoSquareMove = new ChessPosition(myRow + 2 * direction, myCol);
        ChessPosition leftCapture = new ChessPosition(myRow + direction, myCol - 1);
        ChessPosition rightCapture = new ChessPosition(myRow + direction, myCol + 1);

        if (myRow != promotionRow) { // Regular moves
            handlePawnMove(myBoard, myPosition, oneSquareMove, twoSquareMove, startRow);
            handlePawnCapture(myBoard, myPosition, leftCapture, opponentColor);
            handlePawnCapture(myBoard, myPosition, rightCapture, opponentColor);
        } else { // Promotion
            handlePromotion(myBoard, myPosition, oneSquareMove, leftCapture, rightCapture);
        }
    }

    private void handlePawnMove(ChessBoard board, ChessPosition myPosition, ChessPosition oneSquareMove, ChessPosition twoSquareMove, int startRow) {
        if (board.getPiece(oneSquareMove) == null) {
            pawnMoves.add(new ChessMove(myPosition, oneSquareMove, null));
            if (myPosition.getRow() == startRow && board.getPiece(twoSquareMove) == null) {
                pawnMoves.add(new ChessMove(myPosition, twoSquareMove, null));
            }
        }
    }

    private void handlePawnCapture(ChessBoard board, ChessPosition myPosition, ChessPosition capturePosition, ChessGame.TeamColor opponentColor) {
        if (capturePosition.getColumn() >= 1 && capturePosition.getColumn() <= 8) {
            ChessPiece targetPiece = board.getPiece(capturePosition);
            if (targetPiece != null && targetPiece.getTeamColor() == opponentColor) {
                pawnMoves.add(new ChessMove(myPosition, capturePosition, null));
            } else if (isEnPassantCapture(board, myPosition, capturePosition, opponentColor)) {
                pawnMoves.add(new ChessMove(myPosition, capturePosition, null, true));
            }
        }
    }

    private boolean isEnPassantCapture(ChessBoard board, ChessPosition myPosition, ChessPosition capturePosition, ChessGame.TeamColor opponentColor) {
        ChessPosition adjacentPawnPos = new ChessPosition(myPosition.getRow(), capturePosition.getColumn());
        ChessPiece adjacentPawn = board.getPiece(adjacentPawnPos);
        return adjacentPawn != null && adjacentPawn.getTeamColor() == opponentColor && adjacentPawn.pawnJustDoubleMoved;
    }

    private void handlePromotion(ChessBoard board, ChessPosition myPosition, ChessPosition oneSquareMove, ChessPosition leftCapture, ChessPosition rightCapture) {
        if (board.getPiece(oneSquareMove) == null) {
            addAllPromotions(myPosition, oneSquareMove);
        }
        if (leftCapture.getColumn() >= 1) {
            handlePromotionCapture(board, myPosition, leftCapture);
        }
        if (rightCapture.getColumn() <= 8) {
            handlePromotionCapture(board, myPosition, rightCapture);
        }
    }

    private void handlePromotionCapture(ChessBoard board, ChessPosition myPosition, ChessPosition capturePosition) {
        ChessPiece targetPiece = board.getPiece(capturePosition);
        if (targetPiece != null && targetPiece.getTeamColor() != board.getPiece(myPosition).getTeamColor()) {
            addAllPromotions(myPosition, capturePosition);
        }
    }

    private void addAllPromotions(ChessPosition myPosition, ChessPosition targetPosition) {
        pawnMoves.add(new ChessMove(myPosition, targetPosition, ChessPiece.PieceType.QUEEN));
        pawnMoves.add(new ChessMove(myPosition, targetPosition, ChessPiece.PieceType.KNIGHT));
        pawnMoves.add(new ChessMove(myPosition, targetPosition, ChessPiece.PieceType.ROOK));
        pawnMoves.add(new ChessMove(myPosition, targetPosition, ChessPiece.PieceType.BISHOP));
    }

    public Collection<ChessMove> getPawnMoves() {
        return this.pawnMoves;
    }
}

