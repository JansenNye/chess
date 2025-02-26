package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessPiece that = (ChessPiece) o;
        return teamColor == that.teamColor && pieceType == that.pieceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamColor, pieceType);
    }

    private final ChessGame.TeamColor teamColor;
    private final PieceType pieceType;
    public boolean hasMoved;
    public boolean pawnJustDoubleMoved;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.teamColor = pieceColor;
        this.pieceType = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return this.teamColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return this.pieceType;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }
    //Finds moves for Queen, Rook and Bishop
    public static Collection<ChessMove> findDistanceMoves(ChessBoard board, ChessPosition myPosition, int[][] directions) {
        Collection<ChessMove> validMoves = new ArrayList<>();
        for (int[] direction : directions) {
            int row = myPosition.getRow();
            int col = myPosition.getColumn();
            while (true) {
                row += direction[0];
                col += direction[1];
                if (row < 1 || row > 8 || col < 1 || col > 8) {
                    break;
                }
                ChessPosition newPosition = new ChessPosition(row, col);
                ChessPiece occupyingPiece = board.getPiece(newPosition);
                if (occupyingPiece == null) { //Empty square, go ahead
                    validMoves.add(new ChessMove(myPosition, newPosition, null));
                } else {
                    if (occupyingPiece.getTeamColor() != board.getPiece(myPosition).getTeamColor()) {
                        validMoves.add(new ChessMove(myPosition, newPosition, null));
                    } break; //Same-color piece is in the way
                }
            }
        } return validMoves;
    }

    //Finds moves for King and Knight
    public static Collection<ChessMove> findOtherMoves(ChessBoard board, ChessPosition myPosition, ChessPosition[] positions) {
        Collection<ChessMove> validMoves = new ArrayList<>();
        for (ChessPosition position : positions) {
            if (position.getRow() >= 1 && position.getRow() <= 8 && position.getColumn() >= 1 && position.getColumn() <= 8) { //In-bounds
                ChessPiece occupyingPiece = board.getPiece(position);
                if (occupyingPiece == null) { //No piece at target square
                    validMoves.add(new ChessMove(myPosition, position, null));
                } else if (occupyingPiece.getTeamColor() != board.getPiece(myPosition).getTeamColor()) { //Capture opponent's piece on target square
                    validMoves.add(new ChessMove(myPosition, position, null));
                }
            }
        } return validMoves;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        return switch (this.pieceType) {
            case KING -> {
                KingPieceMoves myKingMoves = new KingPieceMoves(myPosition, board);
                yield myKingMoves.getKingMoves();
            } case QUEEN -> {
                QueenPieceMoves myQueenMoves = new QueenPieceMoves(myPosition, board);
                yield myQueenMoves.getQueenMoves();
            } case BISHOP -> {
                BishopPieceMoves bishopMoves = new BishopPieceMoves(myPosition, board);
                yield bishopMoves.getBishopMoves();
            } case KNIGHT -> {
                KnightPieceMoves knightMoves = new KnightPieceMoves(myPosition, board);
                yield knightMoves.getKnightMoves();
            } case ROOK -> {
                RookPieceMoves rookMoves = new RookPieceMoves(myPosition, board);
                yield rookMoves.getRookMoves();
            } case PAWN -> {
                PawnPieceMoves pawnMoves = new PawnPieceMoves(myPosition, board);
                yield pawnMoves.getPawnMoves();
            }
        };
    }

    public void setPawnJustDoubleMoved(boolean pawnJustDoubleMoved) {
        this.pawnJustDoubleMoved = pawnJustDoubleMoved;
    }

}

