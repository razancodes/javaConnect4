import java.util.Random;

public class Connect4Bot {
    private static final int MAX_DEPTH = 10; // Restricted depth as per plan

    // Bot assumes it is playing as Yellow (false)
    // Red (true) is the player (minimizing opponent)
    
    public int getBestMove(GameState state) {
        int bestMove = -1;
        int bestValue = Integer.MIN_VALUE;
        
        // Iterate through all possible columns (1-7)
        for (int col = 1; col <= 7; col++) {
            if (!state.isColumnFull(col)) {
                // Create a hypothetical state for this move
                GameState nextState = new GameState(state);
                nextState.move(col);
                
                // Call minimax for the resulting state
                // Since bot made a move, it's now Red's turn (minimizing)
                int value = minimax(nextState, MAX_DEPTH - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
                
                if (value > bestValue) {
                    bestValue = value;
                    bestMove = col;
                }
            }
        }
        
        // Fallback if no valid moves (shouldn't happen unless board full)
        if (bestMove == -1) {
            for (int col = 1; col <= 7; col++) {
               if (!state.isColumnFull(col)) return col;
            }
        }
        
        return bestMove;
    }

    // isMaximizing: true if it's Bot's turn (Yellow), false if Player's turn (Red)
    // Actually, careful here. In recursions:
    // When we call minimax, we have just made a move.
    // If we just made a move as Bot (Yellow), it is now Red's turn. Red wants to MINIMIZE score.
    // So isMaximizing should represent whose turn it is to move in the *simulated* state?
    // Let's standardise: 
    // isMaximizing = true -> It's currently Bot's turn to move in this state (Yellow).
    // isMaximizing = false -> It's currently Player's turn to move in this state (Red).
    
    // BUT, the initial call in getBestMove simulates the Bot moving. 
    // So the recursive call passes `false` (Player's turn).
    
    private int minimax(GameState state, int depth, int alpha, int beta, boolean isMaximizing) {
        if (depth == 0 || state.getGameOver()) {
            return evaluate(state, depth); // Pass depth to prioritize faster wins
        }

        if (isMaximizing) { // Bot's turn (Yellow)
            int maxEval = Integer.MIN_VALUE;
            // Optimization: Search center columns first (see section 3)
            for (int col : new int[]{4, 3, 5, 2, 6, 1, 7}) { 
                if (!state.isColumnFull(col)) {
                    GameState nextState = new GameState(state);
                    nextState.move(col);
                    int eval = minimax(nextState, depth - 1, alpha, beta, false);
                    maxEval = Math.max(maxEval, eval);
                    alpha = Math.max(alpha, eval);
                    if (beta <= alpha) break; // Beta Cut-off
                }
            }
            return maxEval;
        } else { // Player's turn (Red)
            int minEval = Integer.MAX_VALUE;
            for (int col : new int[]{4, 3, 5, 2, 6, 1, 7}) {
                if (!state.isColumnFull(col)) {
                    GameState nextState = new GameState(state);
                    nextState.move(col);
                    int eval = minimax(nextState, depth - 1, alpha, beta, true);
                    minEval = Math.min(minEval, eval);
                    beta = Math.min(beta, eval);
                    if (beta <= alpha) break; // Alpha Cut-off
                }
            }
            return minEval;
        }
    }

    // Simple heuristic evaluation
    private int evaluate(GameState state, int depth) {
        if (state.getYellowWins()) return 100000 + depth; // Win faster
        if (state.getRedWins()) return -100000 - depth;   // Lose slower
        
        int score = 0;
        Boolean[][] pieces = state.getPieces();
        
        // Evaluate horizontal, vertical, and diagonal windows of 4 cells
        // Example: Horizontal check
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 4; c++) {
                score += evaluateWindow(pieces[c][r], pieces[c+1][r], pieces[c+2][r], pieces[c+3][r]);
            }
        }
        // (Repeat similar loops for Vertical and Diagonals)
        return score;
    }

    private int evaluateWindow(Boolean... cells) {
        int botCount = 0;
        int playerCount = 0;
        
        for (Boolean cell : cells) {
            if (cell != null) {
                if (cell == false) botCount++; // Yellow
                else playerCount++;            // Red
            }
        }

        // Only score windows that aren't blocked by the opponent
        if (botCount > 0 && playerCount == 0) {
            if (botCount == 3) return 100;
            if (botCount == 2) return 10;
        } else if (playerCount > 0 && botCount == 0) {
            if (playerCount == 3) return -100; // Block this!
            if (playerCount == 2) return -10;
        }
        return 0;
    }

}
