package chess;

import java.util.Collection;
import java.util.Iterator;

import static java.lang.Math.abs;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {
    private TeamColor teamTurn = TeamColor.WHITE;
    private ChessBoard board = new ChessBoard();
    private ChessPosition whiteKingPosition;
    private ChessPosition blackKingPosition;
    private ChessPiece storedMovedPiece;
    private ChessPiece storedCapturedPiece;
    public ChessGame() {
        board.resetBoard();
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
        }
        Collection<ChessMove> potentialMoves = piece.pieceMoves(this.board, startPosition);
        Iterator<ChessMove> iterator = potentialMoves.iterator();
        while (iterator.hasNext()) {
            ChessMove move = iterator.next();
            if (piece.getPieceType() == ChessPiece.PieceType.KING &&
                    abs(move.getEndPosition().getColumn() - startPosition.getColumn()) == 2) { //Castling
                if (!castlingValid(move)) { iterator.remove(); }
            } else {
                tryMove(move);
                if ((piece.getTeamColor() == TeamColor.WHITE && staticIsInCheck(whiteKingPosition, this.board)) |
                        (piece.getTeamColor() == TeamColor.BLACK && staticIsInCheck(blackKingPosition, this.board))) {
                    iterator.remove();
                }
                undoMove(move);
            }
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
        Collection<ChessMove> potentialMoves = validMoves(startPosition);
        if (potentialMoves == null) {
            throw new InvalidMoveException("No valid moves for piece");
        }
        if (!potentialMoves.contains(move)) {
            throw new InvalidMoveException("Invalid move");
        }
        tryMove(move);
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
        if (piece.getPieceType() == ChessPiece.PieceType.KING) {
            if (abs(startPosition.getColumn() - endPosition.getColumn()) == 2) { //Castling
                int rookStartCol = move.getEndPosition().getColumn() == 7 ? 8 : 1;
                int rookEndCol = move.getEndPosition().getColumn() == 7 ? 6 : 4;
                ChessPosition rookStart = new ChessPosition(move.getStartPosition().getRow(), rookStartCol);
                ChessPosition rookEnd = new ChessPosition(move.getStartPosition().getRow(), rookEndCol);
                ChessPiece rook = board.getPiece(rookStart);
                board.addPiece(rookEnd, rook);
                board.addPiece(rookStart, null);
                rook.setHasMoved(true);
            }
        } for (int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition pawnPos = new ChessPosition(i, j);
                ChessPiece pawnPiece = board.getPiece(pawnPos);
                if (pawnPiece != null && pawnPiece.getPieceType() == ChessPiece.PieceType.PAWN) {
                    pawnPiece.setPawnJustDoubleMoved(false);
                }
            }
        } if (piece.getPieceType() == ChessPiece.PieceType.PAWN) {
            piece.setPawnJustDoubleMoved(abs(startPosition.getRow() - endPosition.getRow()) == 2);
        } this.teamTurn = this.teamTurn == TeamColor.WHITE ? TeamColor.BLACK : TeamColor.WHITE; //Set team turn
        piece.setHasMoved(true); //Mark the piece as having moved
        move.setIsEnPassant();
    }

    public void undoMove(ChessMove move) {
        this.board.addPiece(move.getStartPosition(), this.storedMovedPiece);
        if (move.getIsEnPassant()) {
            this.board.addPiece(new ChessPosition(move.getStartPosition().getRow(), move.getEndPosition().getColumn()), this.storedCapturedPiece);
            this.board.addPiece(new ChessPosition(move.getEndPosition().getRow(), move.getEndPosition().getColumn()), null);
        } else {
            this.board.addPiece(move.getEndPosition(), this.storedCapturedPiece);
            if (this.board.getPiece(move.getStartPosition()).getPieceType() == ChessPiece.PieceType.KING) {
                if (teamTurn == TeamColor.WHITE) {
                    whiteKingPosition = move.getStartPosition();
                } else {
                    blackKingPosition = move.getStartPosition();
                }
            }
        } getKingPosition(teamTurn == TeamColor.WHITE ? TeamColor.WHITE : TeamColor.BLACK);
    }

    public void tryMove(ChessMove move) {
        this.storedMovedPiece = this.board.getPiece(move.getStartPosition());
        // Check for en passant first
        ChessPosition enPassantPosition = new ChessPosition(move.getStartPosition().getRow(), move.getEndPosition().getColumn());
        ChessPiece enPassantPawn = board.getPiece(enPassantPosition);
        if (move.getIsEnPassant() | (enPassantPawn != null && enPassantPawn.getPieceType() == ChessPiece.PieceType.PAWN
                && enPassantPawn.pawnJustDoubleMoved)) {
            this.storedCapturedPiece = this.board.getPiece(enPassantPosition);
            this.board.addPiece(enPassantPosition, null);
            this.board.addPiece(move.getEndPosition(), this.storedMovedPiece);
            this.board.addPiece(move.getStartPosition(), null);
        } else { //Not en passant
            this.storedCapturedPiece = this.board.getPiece(move.getEndPosition());
            if (move.getPromotionPiece() != null) {
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
            }
        } getKingPosition(TeamColor.WHITE);
        getKingPosition(TeamColor.BLACK);
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

    private boolean castlingValid(ChessMove move){
        ChessPosition startPosition = move.getStartPosition();
        ChessPosition endPosition = move.getEndPosition();
        ChessPosition kingFirstSquare = new ChessPosition(startPosition.getRow(), (startPosition.getColumn() + endPosition.getColumn()) / 2);
        ChessPiece middlePiece = board.getPiece(kingFirstSquare);
        ChessPiece endPiece = board.getPiece(endPosition);
        if (middlePiece != null | endPiece != null) {
            return false;
        }
        ChessMove firstKingMove = new ChessMove(startPosition, kingFirstSquare, null); //First of two king moves in castling
        tryMove(firstKingMove);
        if (staticIsInCheck(kingFirstSquare, board)) {
            undoMove(firstKingMove);
            return false;
        } if (endPosition.getColumn() == 7) { //Queen-side castling
            ChessPosition thirdPiecePosition = new ChessPosition(endPosition.getRow(), 2);
            ChessPiece thirdPiece = board.getPiece(thirdPiecePosition);
            if (thirdPiece != null) {
                undoMove(firstKingMove);
                return false;
            }
        }
        ChessMove secondKingMove = new ChessMove(kingFirstSquare, endPosition, null);
        tryMove(secondKingMove);
        if (staticIsInCheck(endPosition, board)) {
            undoMove(secondKingMove);
            undoMove(firstKingMove);
            return false;
        }
        undoMove(secondKingMove);
        undoMove(firstKingMove);
        return true;
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
        ChessPosition kingPosition;
        if (teamColor == TeamColor.WHITE) {
            kingPosition = whiteKingPosition;
        } else {
            kingPosition = blackKingPosition;
        } return returnCheckmate(kingPosition, teamColor);
    }

    public boolean returnCheckmate(ChessPosition kingPosition, TeamColor teamColor) {
        ChessPiece king = board.getPiece(kingPosition);
        boolean inCheckmate = true;
        Collection<ChessMove> kingMoves = king.pieceMoves(board, kingPosition);
        if (!staticIsInCheck(kingPosition, this.board)) {
            return false;
        } for (ChessMove move : kingMoves) {
            tryMove(move);
            if (!staticIsInCheck(kingPosition, this.board)) {
                inCheckmate = false;
                undoMove(move);
                break;
            } else {
                undoMove(move);
            }
        } for (int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition position = new ChessPosition(i, j);
                ChessPiece piece = board.getPiece(position);
                if (piece == null) {
                    continue;
                } if (piece.getTeamColor() == teamColor) {
                    Collection<ChessMove> pieceMoves = piece.pieceMoves(board, position);
                    for (ChessMove move : pieceMoves) {
                        tryMove(move);
                        if (!staticIsInCheck(kingPosition, this.board)) {
                            inCheckmate = false;
                        } undoMove(move);
                    }
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
        getKingPosition(teamColor);
        if (staticIsInCheck(whiteKingPosition, this.board) || staticIsInCheck(blackKingPosition, this.board)) {
            return false;
        } for (int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition position = new ChessPosition(i, j);
                if (board.getPiece(position) != null && board.getPiece(position).getTeamColor() == teamColor) {
                    validMoves(position);
                    if (!validMoves(position).isEmpty()) {
                        return false;
                    }
                }
            }
        } return true;
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