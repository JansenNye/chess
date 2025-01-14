package chess;

import java.util.Collection;
import java.util.List;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {
    private TeamColor teamTurn;
    private ChessBoard board = new ChessBoard();
    private ChessPosition whiteKingPosition;
    private ChessPosition blackKingPosition;
    private ChessPiece storedMovedPiece;
    private ChessPosition storedMovedPiecePosition;
    private ChessPiece storedCapturedPiece;
    private ChessPosition storedCapturedPiecePosition;
    public ChessGame() {
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return this.teamTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        this.teamTurn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = this.board.getPiece(startPosition);
        if (piece == null) { //No piece at the position
            return null;
        } Collection<ChessMove> potentialMoves = piece.pieceMoves(this.board, startPosition);
        for (ChessMove move : potentialMoves) {
            tryMove(move);
            if (teamTurn == TeamColor.WHITE) {
                if (staticIsInCheck(whiteKingPosition, this.board)) {
                    potentialMoves.remove(move);
                }
            } else {
                if (staticIsInCheck(blackKingPosition, this.board)) {
                    potentialMoves.remove(move);
                }
            } undoMove(move);
        } return potentialMoves;
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to preform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPosition startPosition = move.getStartPosition();
        ChessPosition endPosition = move.getEndPosition();
        ChessPiece piece = this.board.getPiece(startPosition);
        boolean useDistance = false;
        if (piece == null) {
            throw new InvalidMoveException("No piece at start position");
        }
        TeamColor teamToMove = piece.getTeamColor();
        if (this.teamTurn != teamToMove) {
            throw new InvalidMoveException("Not your turn");
        }
        if (this.board.getPiece(endPosition) != null && this.board.getPiece(endPosition).getTeamColor() == teamToMove) {
            throw new InvalidMoveException("Cannot take own piece");
        }
        int[][] directions = switch (piece.getPieceType()) {
            case BISHOP -> {
                useDistance = true;
                yield new int[][]{
                        {1, 1},   // Up and right
                        {1, -1},  // Up and left
                        {-1, 1},  // Down and right
                        {-1, -1}, // Down and left
                };
            } case ROOK -> {
                useDistance = true;
                yield new int[][]{
                        {1, 0},  //Up
                        {-1, 0}, //Down
                        {0, 1},  //Right
                        {0, -1}, //Left
                };
            } case QUEEN -> {
                useDistance = true;
                yield new int[][]{
                        {1, 1},   // Up and right
                        {1, 0},   // Up
                        {1, -1},  // Up and left
                        {0, 1},   // Right
                        {0, -1},  // Left
                        {-1, 1},  // Down and right
                        {-1, 0},  // Down
                        {-1, -1}, // Down and left
                };
            } default -> null;
        }; Collection<ChessMove> moveCollection = List.of();
        if (useDistance) {
            moveCollection = ChessPiece.findDistanceMoves(board, startPosition, directions);
            if (!moveCollection.contains(move)) {
                throw new InvalidMoveException("Invalid move");
            }
        } tryMove(move);
        if (teamToMove == TeamColor.WHITE) {
            if (staticIsInCheck(whiteKingPosition, this.board)) {
                undoMove(move);
                throw new InvalidMoveException("You are in check");
            }
        } else {
            if (staticIsInCheck(blackKingPosition, this.board)) {
                undoMove(move);
                throw new InvalidMoveException("You are in check");
            }
        }
        this.teamTurn = this.teamTurn == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE; //Set team turn
        piece.setHasMoved(true); //Mark the piece as having moved
        if (piece.getPieceType() == ChessPiece.PieceType.KING) {
            if (Math.abs(startPosition.getColumn() - endPosition.getColumn()) == 2) { //Castling
                ChessPosition intermediatePosition = new ChessPosition(startPosition.getRow(),
                        (startPosition.getColumn() + endPosition.getColumn()) / 2);
                if (ChessGame.staticIsInCheck(intermediatePosition, board)) {
                    undoMove(move);
                    throw new InvalidMoveException("Cannot castle through check");
                }
                int rookStartCol = move.getEndPosition().getColumn() == 7 ? 8 : 1;
                int rookEndCol = move.getEndPosition().getColumn() == 7 ? 6 : 4;
                ChessPosition rookStart = new ChessPosition(move.getStartPosition().getRow(), rookStartCol);
                ChessPosition rookEnd = new ChessPosition(move.getStartPosition().getRow(), rookEndCol);
                ChessPiece rook = board.getPiece(rookStart);
                board.addPiece(rookEnd, rook);
                board.addPiece(rookStart, null);
                rook.setHasMoved(true);
            }
        }
        for (int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition pos = new ChessPosition(i, j);
                ChessPiece p = board.getPiece(pos);
                if (p != null && p.getPieceType() == ChessPiece.PieceType.PAWN) {
                    p.setPawnJustDoubleMoved(false);
                }
            }
        }
        if (piece.getPieceType() == ChessPiece.PieceType.PAWN) {
            piece.setPawnJustDoubleMoved(startPosition.getRow() - endPosition.getRow() == 2);
        }
    }

    public void undoMove(ChessMove move) {
        this.board.addPiece(move.getStartPosition(), this.storedMovedPiece);
        this.board.addPiece(move.getEndPosition(), this.storedCapturedPiece);
        if (this.board.getPiece(move.getStartPosition()).getPieceType() == ChessPiece.PieceType.KING) {
            if (teamTurn == TeamColor.WHITE) {
                whiteKingPosition = move.getStartPosition();
            } else {
                blackKingPosition = move.getStartPosition();
            }
        } getKingPosition(teamTurn == TeamColor.WHITE ? TeamColor.WHITE : TeamColor.BLACK);
    }

    public void tryMove(ChessMove move) {
        this.storedMovedPiece = this.board.getPiece(move.getStartPosition());
        ChessPiece capturedPiece = this.board.getPiece(move.getEndPosition());
        if (capturedPiece != null) {
            this.storedCapturedPiece = capturedPiece;
            this.storedCapturedPiecePosition = move.getEndPosition();
        } if (move.getPromotionPiece() != null) {
            this.board.addPiece(move.getEndPosition(), new ChessPiece(this.teamTurn, move.getPromotionPiece()));
        } else {
            this.board.addPiece(move.getEndPosition(), this.board.getPiece(move.getStartPosition()));
        } this.board.addPiece(move.getStartPosition(), null);
        if (this.board.getPiece(move.getEndPosition()).getPieceType() == ChessPiece.PieceType.KING) {
            if (teamTurn == TeamColor.WHITE) {
                whiteKingPosition = move.getEndPosition();
            } else {
                blackKingPosition = move.getEndPosition();
            }
        } getKingPosition(teamTurn == TeamColor.WHITE ? TeamColor.WHITE : TeamColor.BLACK);
    }

    public void getKingPosition(TeamColor teamColor) {
        for (int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition position = new ChessPosition(i, j);
                ChessPiece piece = this.board.getPiece(position);
                if (piece != null) {
                    if (piece.getPieceType() == ChessPiece.PieceType.KING && piece.getTeamColor() == teamColor) {
                        if (teamColor == TeamColor.WHITE) {
                            whiteKingPosition = position;
                        } else {
                            blackKingPosition = position;
                        } break;
                    }
                }
            }
        }
    }


    public static boolean staticIsInCheck(ChessPosition kingPosition, ChessBoard board) {
        for (int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition position = new ChessPosition(i, j);
                ChessPiece piece = board.getPiece(position);
                if (piece == null) {
                    continue;
                } Collection<ChessMove> pieceMoves = piece.pieceMoves(board, position);
                for (ChessMove move : pieceMoves) {
                    if (move.getEndPosition().equals(kingPosition) && piece.getTeamColor() != board.getPiece(move.getEndPosition()).getTeamColor()) {
                        return true;
                    }
                }
            }
        } return false;
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        getKingPosition(teamColor);
        for (int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition position = new ChessPosition(i, j);
                ChessPiece piece = board.getPiece(position);
                if (piece == null) {
                    continue;
                } Collection<ChessMove> pieceMoves = piece.pieceMoves(board, position);
                for (ChessMove move : pieceMoves) {
                    if (teamColor == TeamColor.WHITE) {
                        if (move.getEndPosition().equals(whiteKingPosition) && piece.getTeamColor() != TeamColor.WHITE) {
                            return true;
                        }
                    } else {
                        if (move.getEndPosition().equals(blackKingPosition) && piece.getTeamColor() != TeamColor.BLACK) {
                            return true;
                        }
                    }
                }
            }
        } return false;
    }
    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        getKingPosition(teamColor);
        boolean inCheckmate = true;
        ChessPiece king;
        Collection<ChessMove> kingMoves;
        if (teamColor == TeamColor.WHITE) {
            king = board.getPiece(whiteKingPosition);
            kingMoves = king.pieceMoves(board, whiteKingPosition);
            if (!staticIsInCheck(whiteKingPosition, this.board)) {
                return false;
            } for (ChessMove move : kingMoves) {
                tryMove(move);
                if (!staticIsInCheck(whiteKingPosition, this.board)) {
                    inCheckmate = false;
                    undoMove(move);
                    break;
                } else {
                    undoMove(move);
                }
            }
        } else {
            king = board.getPiece(blackKingPosition);
            kingMoves = king.pieceMoves(board, blackKingPosition);
            if (!staticIsInCheck(blackKingPosition, this.board)) {
                return false;
            } for (ChessMove move : kingMoves) {
                tryMove(move);
                if (!staticIsInCheck(blackKingPosition, this.board)) {
                    inCheckmate = false;
                    undoMove(move);
                    break;
                } else {
                    undoMove(move);
                }
            }
        } return inCheckmate;
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate (TeamColor teamColor){
        if (staticIsInCheck(whiteKingPosition, this.board) || staticIsInCheck(blackKingPosition, this.board)) {
            return false;
        }
        for (int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition position = new ChessPosition(i, j);
                validMoves(position);
                if (validMoves(position) != null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard (ChessBoard board){
        this.board = board;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return this.board;
    }
}