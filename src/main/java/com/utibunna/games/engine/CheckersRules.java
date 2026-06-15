package com.utibunna.games.engine;

import com.utibunna.games.model.Move;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure move math for American/English draughts. No state, no Spring — easy to unit-test.
 *
 * <p>Board encoding: 0 empty, 1 = HOST man, 2 = GUEST man, 3 = HOST king, 4 = GUEST king.
 * HOST starts at the bottom (rows 5-7) moving up (decreasing row); GUEST at the top (rows 0-2)
 * moving down. Men move/capture diagonally forward only; kings move/capture one square any diagonal
 * (short-range, not flying). Captures are mandatory.</p>
 */
public final class CheckersRules {

    public static final int SIZE = 8;

    private CheckersRules() {
    }

    public static boolean inBounds(int r, int c) {
        return r >= 0 && r < SIZE && c >= 0 && c < SIZE;
    }

    public static boolean isHost(int piece) {
        return piece == 1 || piece == 3;
    }

    public static boolean isGuest(int piece) {
        return piece == 2 || piece == 4;
    }

    public static boolean isKing(int piece) {
        return piece == 3 || piece == 4;
    }

    public static boolean isOwn(int piece, boolean host) {
        return host ? isHost(piece) : isGuest(piece);
    }

    public static boolean isEnemy(int piece, boolean host) {
        return host ? isGuest(piece) : isHost(piece);
    }

    /** Diagonal directions a piece may travel: men forward only, kings all four. */
    private static int[][] directions(int piece) {
        if (piece == 1) {
            return new int[][]{{-1, -1}, {-1, 1}}; // HOST man: up
        }
        if (piece == 2) {
            return new int[][]{{1, -1}, {1, 1}};   // GUEST man: down
        }
        return new int[][]{{-1, -1}, {-1, 1}, {1, -1}, {1, 1}}; // king
    }

    /** Single capture moves available for the piece at (r,c). */
    public static List<Move> jumpsFrom(int[][] b, int r, int c) {
        List<Move> out = new ArrayList<>();
        int piece = b[r][c];
        if (piece == 0) {
            return out;
        }
        boolean host = isHost(piece);
        for (int[] d : directions(piece)) {
            int mr = r + d[0], mc = c + d[1];
            int lr = r + 2 * d[0], lc = c + 2 * d[1];
            if (inBounds(lr, lc) && b[lr][lc] == 0 && isEnemy(b[mr][mc], host)) {
                out.add(coord(r, c, lr, lc));
            }
        }
        return out;
    }

    /** Non-capturing steps available for the piece at (r,c). */
    public static List<Move> stepsFrom(int[][] b, int r, int c) {
        List<Move> out = new ArrayList<>();
        int piece = b[r][c];
        if (piece == 0) {
            return out;
        }
        for (int[] d : directions(piece)) {
            int tr = r + d[0], tc = c + d[1];
            if (inBounds(tr, tc) && b[tr][tc] == 0) {
                out.add(coord(r, c, tr, tc));
            }
        }
        return out;
    }

    /** All legal moves for a side. If any capture exists, only captures are returned (mandatory). */
    public static List<Move> legalMoves(int[][] b, boolean host) {
        List<Move> jumps = new ArrayList<>();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (isOwn(b[r][c], host)) {
                    jumps.addAll(jumpsFrom(b, r, c));
                }
            }
        }
        if (!jumps.isEmpty()) {
            return jumps;
        }
        List<Move> steps = new ArrayList<>();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (isOwn(b[r][c], host)) {
                    steps.addAll(stepsFrom(b, r, c));
                }
            }
        }
        return steps;
    }

    private static Move coord(int fr, int fc, int tr, int tc) {
        return new Move(fr, fc, tr, tc, null, null, null);
    }
}
