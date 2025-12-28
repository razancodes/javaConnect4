import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.util.concurrent.ExecutionException;

public class Connect4UI {
    
    // Theme Colors
    private static final Color COLOR_BG = new Color(236, 240, 241);
    private static final Color COLOR_BTN_BLUE = new Color(52, 152, 219);
    private static final Color COLOR_BTN_GREEN = new Color(46, 204, 113);
    private static final Color COLOR_BTN_TEXT = Color.WHITE;
    
    private static boolean isBotMode = false;
    private static final Connect4Bot bot = new Connect4Bot();
    private static boolean botThinking = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Connect 4");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBackground(COLOR_BG);
        frame.setSize(800, 600);
        
        // Main Container with CardLayout
        CardLayout cardLayout = new CardLayout();
        JPanel mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(COLOR_BG);
        frame.setContentPane(mainPanel);

        // --- Start Screen ---
        JPanel startPanel = createStartPanel(cardLayout, mainPanel);
        mainPanel.add(startPanel, "START");

        // --- Game Screen ---
        final GameState state = new GameState();
        final BoardDrawing board = new BoardDrawing(state);
        JPanel gamePanel = createGamePanel(cardLayout, mainPanel, state, board);
        mainPanel.add(gamePanel, "GAME");

        // Show Start Screen initially
        cardLayout.show(mainPanel, "START");

        frame.pack(); // Pack to fit the start screen or preferred size
        // Ensure the frame is at least big enough for the game
        frame.setMinimumSize(new Dimension(800, 720)); 
        frame.setSize(800, 720); // Explicitly set a taller size
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static JPanel createStartPanel(CardLayout cardLayout, JPanel mainPanel) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(COLOR_BG);

        JLabel title = new JLabel("Connect 4");
        title.setFont(new Font("Courier New", Font.BOLD, 48));
        title.setForeground(new Color(44, 62, 80));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0));
        buttonPanel.setBackground(COLOR_BG);

        JButton btnPvp = createStyledButton("PVP", COLOR_BTN_BLUE);
        btnPvp.setPreferredSize(new Dimension(150, 60));
        btnPvp.addActionListener(e -> {
            isBotMode = false;
            cardLayout.show(mainPanel, "GAME");
            // Force layout update just in case
            mainPanel.revalidate();
            mainPanel.repaint();
        });

        JButton btnBot = createStyledButton("BOT", COLOR_BTN_GREEN);
        btnBot.setPreferredSize(new Dimension(150, 60));
        btnBot.addActionListener(e -> {
            isBotMode = true;
            cardLayout.show(mainPanel, "GAME");
            mainPanel.revalidate();
            mainPanel.repaint();
        });

        buttonPanel.add(btnPvp);
        buttonPanel.add(btnBot);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 50, 0);
        panel.add(title, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(buttonPanel, gbc);

        return panel;
    }

    private static JPanel createGamePanel(CardLayout cardLayout, JPanel mainPanel, GameState state, BoardDrawing board) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_BG);

        // Board Area
        JPanel boardPanel = new JPanel(new GridBagLayout());
        boardPanel.setBackground(COLOR_BG);
        boardPanel.add(board);
        panel.add(boardPanel, BorderLayout.CENTER);

        // Controls Area
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
        controlsPanel.setBackground(COLOR_BG);
        controlsPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Column Buttons
        JPanel dropButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        dropButtonsPanel.setBackground(COLOR_BG);
        
        for (int i = 1; i <= 7; i++) {
            final int col = i;
            JButton btn = createStyledButton(String.valueOf(i), COLOR_BTN_BLUE);
            btn.setPreferredSize(new Dimension(50, 40));
            btn.addActionListener(e -> {
                if (botThinking || state.getGameOver()) return; // Prevent moves while bot thinks

                state.move(col);
                board.repaint();
                
                // Trigger Bot if applicable
                if (isBotMode && !state.getGameOver() && !state.getRedsTurn()) {
                    triggerBotMove(state, board);
                }
            });
            dropButtonsPanel.add(btn);
        }

        // Game Control Buttons (Undo, Restart)
        JPanel gameActionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        gameActionPanel.setBackground(COLOR_BG);
        gameActionPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        JButton btnUndo = createStyledButton("Undo", COLOR_BTN_BLUE);
        btnUndo.addActionListener(e -> {
            if (isBotMode) {
                // If it's bot mode, we generally want to undo TWO moves (Bot's move + Player's move)
                // to get back to Player's turn.
                // But check if at least 2 moves exist.
                if (state.getMoves().size() >= 2) {
                    state.undo(); // Undo Bot's move
                    state.undo(); // Undo Player's move
                } else if (state.getMoves().size() == 1) {
                    // Start of game oddity (maybe bot hasn't moved yet?)
                    state.undo();
                } else {
                    // 0 moves, do nothing or show error
                     // Let standard undo handle empty stack error if we want, 
                     // but state.undo() checks internally.
                     state.undo();
                }
            } else {
                state.undo();
            }
            board.repaint();
        });

        JButton btnRestart = createStyledButton("Restart", COLOR_BTN_GREEN);
        btnRestart.addActionListener(e -> {
            state.restart();
            board.repaint();
            cardLayout.show(mainPanel, "START");
        });

        gameActionPanel.add(btnUndo);
        gameActionPanel.add(btnRestart);

        controlsPanel.add(dropButtonsPanel);
        controlsPanel.add(gameActionPanel);
        panel.add(controlsPanel, BorderLayout.SOUTH);

        return panel;
    }

    private static void triggerBotMove(GameState state, BoardDrawing board) {
        botThinking = true;
        // Run bot logic in background to keep UI responsive
        SwingWorker<Integer, Void> worker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                // Small delay for better UX
                // Thread.sleep(500); // removed the delay for deeper search...
                return bot.getBestMove(state);
            }

            @Override
            protected void done() {
                try {
                    int col = get();
                    if (col != -1) {
                        state.move(col);
                        board.repaint();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                } finally {
                    botThinking = false;
                }
            }
        };
        worker.execute();
    }

    private static JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2.setColor(getBackground().darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(getBackground().brighter());
                } else {
                    g2.setColor(getBackground());
                }
                
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                g2.setColor(getForeground());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - 2;
                g2.drawString(getText(), x, y);
                
                g2.dispose();
            }
        };
        
        btn.setFont(new Font("Courier New", Font.BOLD, 14));
        btn.setForeground(COLOR_BTN_TEXT);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
