package chess;

import java.util.Collection;

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
    public ChessGame(TeamColor teamTurn) {
        this.teamTurn = teamTurn;
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
        if(piece == null) {
            return null;
        } Collection<ChessMove> potentialMoves = piece.pieceMoves(this.board, startPosition);
        for (ChessMove move : potentialMoves) {
            tryMove(move);
            if (isInCheck(teamTurn)) {
                potentialMoves.remove(move);
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
        if (this.teamTurn != this.board.getPiece(move.getStartPosition()).getTeamColor()) {
            throw new InvalidMoveException("Not your turn");
        }
        this.board.addPiece(move.getStartPosition(), null); //Remove the piece at the start position
        tryMove(move);
        if (isInCheck(this.teamTurn)) {
            this.board.addPiece(move.getStartPosition(), this.board.getPiece(move.getStartPosition()));
            this.board.addPiece(move.getEndPosition(), null); //Undo move
            throw new InvalidMoveException("You are in check");
        }
    }

    public void undoMove(ChessMove move) {
        this.board.addPiece(move.getStartPosition(), this.board.getPiece(move.getStartPosition()));
        this.board.addPiece(move.getEndPosition(), null);
    }

    public void tryMove(ChessMove move) {
        if (move.getPromotionPiece() != null) {
            this.board.addPiece(move.getEndPosition(), new ChessPiece(this.teamTurn, move.getPromotionPiece()));
        } else {
            this.board.addPiece(move.getEndPosition(), this.board.getPiece(move.getStartPosition()));
        }
    }
    public void getKingPosition(TeamColor teamColor) {
        for(int i = 1; i <= 8; i++) {
            for(int j = 1; j <= 8; j++) {
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
    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        getKingPosition(teamColor);
        for(int i = 1; i <= 8; i++) {
            for(int j = 1; j <= 8; j++) {
                ChessPosition position = new ChessPosition(i, j);
                ChessPiece piece = this.board.getPiece(position);
                Collection<ChessMove> pieceMoves = piece.pieceMoves(this.board, position);
                for (ChessMove move : pieceMoves) {
                    if (teamColor == TeamColor.WHITE) {
                        if (move.getEndPosition() == whiteKingPosition) {
                            return true;
                        }
                    } else {
                        if (move.getEndPosition() == blackKingPosition) {
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
        Collection<ChessMove> kingMoves;
        if (!isInCheck(teamColor)) {
            return false;
        } if (teamColor == TeamColor.WHITE) {
            ChessPiece king = board.getPiece(whiteKingPosition);
            kingMoves = king.pieceMoves(board, whiteKingPosition);
        } else {
            ChessPiece king = board.getPiece(blackKingPosition);
            kingMoves = king.pieceMoves(board, blackKingPosition);
        } for (ChessMove move : kingMoves) {
            tryMove(move);
            if (!isInCheck(teamColor)) {
                inCheckmate = false;
                undoMove(move);
                break;
            } else {
                undoMove(move);
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
    public boolean isInStalemate(TeamColor teamColor) {
        Collection<ChessMove> potentialMoves;
        if (isInCheck(teamColor)) {
            return false;
        } for(int i = 1; i <= 8; i++) {
            for (int j = 1; j <= 8; j++) {
                ChessPosition position = new ChessPosition(i, j);
                validMoves(position);
                if (validMoves(position) != null) {
                    return false;
                }
            }
        }
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
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
