import java.util.Random;

public class Connect4Bot {
    private static final int MAX_DEPTH = 5; // Restricted depth as per plan

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
                int value = minimax(nextState, MAX_DEPTH - 1, false);
                
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
    
    private int minimax(GameState state, int depth, boolean isMaximizing) {
        // Base cases
        if (state.getGameOver()) {
            if (state.getYellowWins()) return 1000 + depth; // Prefer faster wins
            if (state.getRedWins()) return -1000 - depth; // Prefer slower losses
            return 0; // Tie
        }
        
        if (depth == 0) {
            return evaluate(state);
        }

        if (isMaximizing) {
            // Bot's turn (Yellow) -> Maximize
            int maxEval = Integer.MIN_VALUE;
            for (int col = 1; col <= 7; col++) {
                if (!state.isColumnFull(col)) {
                    GameState nextState = new GameState(state);
                    nextState.move(col);
                    int eval = minimax(nextState, depth - 1, false);
                    maxEval = Math.max(maxEval, eval);
                }
            }
            return maxEval == Integer.MIN_VALUE ? 0 : maxEval; // Handle stuck case
        } else {
            // Player's turn (Red) -> Minimize
            int minEval = Integer.MAX_VALUE;
            for (int col = 1; col <= 7; col++) {
                if (!state.isColumnFull(col)) {
                    GameState nextState = new GameState(state);
                    nextState.move(col);
                    int eval = minimax(nextState, depth - 1, true);
                    minEval = Math.min(minEval, eval);
                }
            }
            return minEval == Integer.MAX_VALUE ? 0 : minEval;
        }
    }

    // Simple heuristic evaluation
    private int evaluate(GameState state) {
        int score = 0;
        // Prioritize center column
        Boolean[][] pieces = state.getPieces();
        int centerCol = 3; // Index 3 is column 4 (0,1,2,3,4,5,6)
        for(int r = 0; r < 6; r++) {
            if (pieces[centerCol][r] != null) {
                if (pieces[centerCol][r] == false) score += 3; // Bot (Yellow)
                else score -= 3; // Player (Red)
            }
        }
        // Could add 2-in-a-row and 3-in-a-row checks here for smarter play
        return score;
    }
}
