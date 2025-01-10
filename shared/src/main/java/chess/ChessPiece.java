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

    //Finds moves for Queen, Rook and Bishop
    public Collection<ChessMove> findDistanceMoves(ChessBoard board, ChessPosition myPosition, int[][] directions) {
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
                if (occupyingPiece == null) {
                    validMoves.add(new ChessMove(myPosition, newPosition, null));
                } else {
                    if (occupyingPiece.getTeamColor() != this.teamColor) {
                        validMoves.add(new ChessMove(myPosition, newPosition, null));
                    } break; //Same-color piece is in the way
                }
            }
        } return validMoves;
    }

    //Finds moves for King and Knight
    public Collection<ChessMove> findOtherMoves(ChessBoard board, ChessPosition myPosition, ChessPosition[] positions) {
        Collection<ChessMove> validMoves = new ArrayList<>();
        for (ChessPosition position : positions) {
            if (position.getRow() >= 1 && position.getRow() <= 8 && position.getColumn() >= 1 && position.getColumn() <= 8) { //In-bounds
                ChessPiece occupyingPiece = board.getPiece(position);
                if (occupyingPiece == null) { //No piece at target square
                    validMoves.add(new ChessMove(myPosition, position, null));
                } else if (occupyingPiece.getTeamColor() != this.teamColor) { //Capture opponent's piece on target square
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
        int myRow = myPosition.getRow();
        int myCol = myPosition.getColumn();
        Collection<ChessMove> validMoves = new ArrayList<>();
        switch (this.pieceType) {
            case KING:
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
                validMoves = findOtherMoves(board, myPosition, kingPositions);
                break;
            case QUEEN:
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
                validMoves = findDistanceMoves(board, myPosition, queenDirections);
                break;
            case BISHOP:
                int[][] bishopDirections = {
                        {1, 1},   // Up and right
                        {1, -1},  // Up and left
                        {-1, 1},  // Down and right
                        {-1, -1}, // Down and left
                };
                validMoves = findDistanceMoves(board, myPosition, bishopDirections);
                break;
            case KNIGHT:
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
                validMoves = findOtherMoves(board, myPosition, knightPositions);
                break;
            case ROOK:
                int[][] rookDirections = {
                        {1, 0},  //Up
                        {-1, 0}, //Down
                        {0, 1},  //Right
                        {0, -1}, //Left
                };
                validMoves = findDistanceMoves(board, myPosition, rookDirections);
                break;
            case PAWN:
                Collection<ChessMove> pawnMoves = new ArrayList<>();
                if (this.teamColor == ChessGame.TeamColor.BLACK) { // ! Black pawns - move down board
                    ChessPosition BlackPawnOneSquare = new ChessPosition(myRow - 1, myCol); //Standard move
                    ChessPosition BlackPawnTakeLeft = new ChessPosition(myRow - 1, myCol - 1); //Diagonal capture
                    ChessPosition BlackPawnTakeRight = new ChessPosition(myRow - 1, myCol + 1); //Diagonal capture
                    if (myRow != 2) { // * Pawn is not promoting
                        if (board.getPiece(BlackPawnOneSquare) == null) { // * One square
                            pawnMoves.add(new ChessMove(myPosition, BlackPawnOneSquare, null));
                            if (myRow == 7) { // * First pawn move
                                ChessPosition BlackPawnTwoSquares = new ChessPosition(myRow - 2, myCol); // * Two-square move
                                if (board.getPiece(BlackPawnTwoSquares) == null) { // * Move is available
                                    pawnMoves.add(new ChessMove(myPosition, BlackPawnTwoSquares, null));
                                }
                            }
                        } if (myCol > 1) { // ? Capture left
                            if (board.getPiece(BlackPawnTakeLeft) != null) {
                                if (board.getPiece(BlackPawnTakeLeft).teamColor == ChessGame.TeamColor.WHITE) {
                                    pawnMoves.add(new ChessMove(myPosition, BlackPawnTakeLeft, null));
                                }
                            }
                        } if (myCol < 8) { // ? Capture right
                            if (board.getPiece(BlackPawnTakeRight) != null) {
                                if (board.getPiece(BlackPawnTakeRight).teamColor == ChessGame.TeamColor.WHITE) {
                                    pawnMoves.add(new ChessMove(myPosition, BlackPawnTakeRight, null));
                                }
                            }
                        }
                    } else { // ~ Pawn is promoting
                        pawnPromote(board, myPosition, pawnMoves, BlackPawnOneSquare, BlackPawnTakeLeft, BlackPawnTakeRight);
                    }
                } else { // ! White pawns - move up board
                    ChessPosition WhitePawnOneSquare = new ChessPosition(myRow + 1, myCol); //Standard move
                    ChessPosition WhitePawnTakeLeft = new ChessPosition(myRow + 1, myCol - 1); //Diagonal capture
                    ChessPosition WhitePawnTakeRight = new ChessPosition(myRow + 1, myCol + 1); //Diagonal capture
                    if (myRow != 7) { // * Pawn is not promoting
                        if (board.getPiece(WhitePawnOneSquare) == null) { // * One square
                            pawnMoves.add(new ChessMove(myPosition, WhitePawnOneSquare, null));
                            if (myRow == 2) { // * First pawn move
                                ChessPosition WhitePawnTwoSquares = new ChessPosition(myRow + 2, myCol); // * Two-square move
                                if (board.getPiece(WhitePawnTwoSquares) == null) { // * Move is available
                                    pawnMoves.add(new ChessMove(myPosition, WhitePawnTwoSquares, null));
                                }
                            }
                        } if (myCol > 1) { // ? Capture left
                            if (board.getPiece(WhitePawnTakeLeft) != null) {
                                if (board.getPiece(WhitePawnTakeLeft).teamColor == ChessGame.TeamColor.BLACK) {
                                    pawnMoves.add(new ChessMove(myPosition, WhitePawnTakeLeft, null));
                                }
                            }
                        } if (myCol < 8) { // ? Capture right
                            if (board.getPiece(WhitePawnTakeRight) != null) {
                                if (board.getPiece(WhitePawnTakeRight).teamColor == ChessGame.TeamColor.BLACK) {
                                    pawnMoves.add(new ChessMove(myPosition, WhitePawnTakeRight, null));
                                }
                            }
                        }
                    } else { // ~ Pawn is promoting
                        pawnPromote(board, myPosition, pawnMoves, WhitePawnOneSquare, WhitePawnTakeLeft, WhitePawnTakeRight);
                    }
                } validMoves = pawnMoves;
        } return validMoves;
    }
    //Find possible pawn promotion moves
    public void pawnPromote(ChessBoard board, ChessPosition myPosition, Collection<ChessMove> pawnMoves, ChessPosition PawnOneSquare, ChessPosition PawnTakeLeft, ChessPosition PawnTakeRight) {
        pawnMoves.add(new ChessMove(myPosition, PawnOneSquare, PieceType.QUEEN));
        pawnMoves.add(new ChessMove(myPosition, PawnOneSquare, PieceType.KNIGHT));
        pawnMoves.add(new ChessMove(myPosition, PawnOneSquare, PieceType.ROOK));
        pawnMoves.add(new ChessMove(myPosition, PawnOneSquare, PieceType.BISHOP));
        if(myPosition.getColumn() > 1) {
            pawnPromotionCapture(board, myPosition, pawnMoves, PawnTakeLeft);
        } if(myPosition.getColumn() < 8) {
            pawnPromotionCapture(board, myPosition, pawnMoves, PawnTakeRight);
        }
    }

    //Helper function for pawnPromote handling pawn promotion captures
    public void pawnPromotionCapture(ChessBoard board, ChessPosition myPosition, Collection<ChessMove> pawnMoves, ChessPosition pawnTake) {
        if (board.getPiece(pawnTake) != null) {
            if (board.getPiece(pawnTake).teamColor != board.getPiece(myPosition).teamColor) {
                pawnMoves.add(new ChessMove(myPosition, pawnTake, PieceType.QUEEN));
                pawnMoves.add(new ChessMove(myPosition, pawnTake, PieceType.KNIGHT));
                pawnMoves.add(new ChessMove(myPosition, pawnTake, PieceType.ROOK));
                pawnMoves.add(new ChessMove(myPosition, pawnTake, PieceType.BISHOP));
            }
        }
    }
}

