package chess;

import java.util.Objects;

/**
 * Represents moving a chess piece on a chessboard
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */

public class ChessMove {
    private final ChessPosition startPosition;
    private final ChessPosition endPosition;
    private final ChessPiece.PieceType promotionPiece;
    private boolean isEnPassant;

    //Existing constructor with default isEnPassant = false
    public ChessMove(ChessPosition startPosition, ChessPosition endPosition,
                     ChessPiece.PieceType promotionPiece) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.promotionPiece = promotionPiece;
        this.isEnPassant = false;
    }

    //Constructor override for specifying isEnPassant
    public ChessMove(ChessPosition startPosition, ChessPosition endPosition,
                     ChessPiece.PieceType promotionPiece, boolean isEnPassant) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.promotionPiece = promotionPiece;
        this.isEnPassant = isEnPassant;
    }

        /**
         * @return ChessPosition of starting location
         */
        public ChessPosition getStartPosition () {
            return this.startPosition;
        }

        /**
         * @return ChessPosition of ending location
         */
        public ChessPosition getEndPosition () {
            return this.endPosition;
        }

        /**
         * Gets the type of piece to promote a pawn to if pawn promotion is part of this
         * chess move
         *
         * @return Type of piece to promote a pawn to, or null if no promotion
         */
        public ChessPiece.PieceType getPromotionPiece () {
            return this.promotionPiece;
        }

        public boolean getIsEnPassant () {
            return this.isEnPassant;
        }

        public void setIsEnPassant () {
            this.isEnPassant = true;
        }
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessMove chessMove = (ChessMove) o;
        return Objects.equals(startPosition, chessMove.startPosition)
                && Objects.equals(endPosition, chessMove.endPosition)
                && promotionPiece == chessMove.promotionPiece;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startPosition, endPosition, promotionPiece, isEnPassant);
    }
}
