package com.stockfish.engine.test;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class PlayVsAiActivity extends AppCompatActivity {

    private StockfishEngine engine;
    private StockfishOutputParser parser;
    private ChessBoardView boardView;

    private TextView evalBarText;
    private View evalBarWhite;
    private ValueAnimator evalBarAnimator;
    private int currentEvalBarWidth = -1;

    private TextView statusText;
    private SeekBar skillSeekBar;
    private TextView skillLevelText;

    private String currentFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    private boolean playerIsWhite = true;
    private boolean gameActive = false;
    private boolean aiThinking = false;

    private int[] selectedSquare = null;
    private java.util.List<int[]> legalDestinations = null;
    private int[] lastMoveSquares = null;
    private int skillLevel = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_vs_ai);

        evalBarText = findViewById(R.id.evalBarText);
        evalBarWhite = findViewById(R.id.evalBarWhite);
        statusText = findViewById(R.id.statusText);
        skillSeekBar = findViewById(R.id.skillSeekBar);
        skillLevelText = findViewById(R.id.skillLevelText);

        FrameLayout piecesContainer = findViewById(R.id.piecesContainer);
        FrameLayout highlightContainer = findViewById(R.id.highlightContainer);
        FrameLayout coordinateContainer = findViewById(R.id.coordinateContainer);
        FrameLayout arrowContainer = findViewById(R.id.arrowContainer);
        FrameLayout interactionContainer = findViewById(R.id.interactionContainer);
        FrameLayout boardContainer = findViewById(R.id.boardContainer);

        Button playWhiteButton = findViewById(R.id.playWhiteButton);
        Button playBlackButton = findViewById(R.id.playBlackButton);

        parser = new StockfishOutputParser();
        boardView = new ChessBoardView(this, piecesContainer, highlightContainer, coordinateContainer, arrowContainer, interactionContainer);
        boardView.setOnSquareClickListener(this::handleSquareTap);

        boardContainer.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        boardContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        redrawBoard();
                    }
                });

        engine = new StockfishEngine(getApplicationContext());
        engine.start();

        engine.readOutput(line -> runOnUiThread(() -> {
            parser.processLine(line);
            if (line.startsWith("bestmove")) {
                handleAiMoveResult(line);
            }
            if (line.startsWith("info depth") && line.contains("score")) {
                updateEvalBar(parser.getEvalInPawns());
            }
        }));

        engine.sendCommand("uci");
        engine.sendCommand("setoption name Threads value 1");
        engine.sendCommand("isready");

        skillSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                skillLevel = progress;
                skillLevelText.setText(String.valueOf(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        playWhiteButton.setOnClickListener(v -> startNewGame(true));
        playBlackButton.setOnClickListener(v -> startNewGame(false));
    }

    private void startNewGame(boolean asWhite) {
        playerIsWhite = asWhite;
        currentFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        selectedSquare = null;
        legalDestinations = null;
        lastMoveSquares = null;
        gameActive = true;
        aiThinking = false;

        engine.sendCommand("setoption name Skill Level value " + skillLevel);

        redrawBoard();
        updateStatus();

        if (!playerIsWhite) {
            triggerAiMove();
        }
    }

    private void handleSquareTap(int row, int col) {
        if (!gameActive || aiThinking) return;

        boolean whiteToMove = currentFen.contains(" w ");
        if (whiteToMove != playerIsWhite) return; // hindi turn ng player

        char[][] board = BoardAnalyzer.parseBoard(currentFen);

        if (selectedSquare == null) {
            char piece = board[row][col];
            if (piece == '.') return;
            boolean pieceIsWhite = Character.isUpperCase(piece);
            if (pieceIsWhite != playerIsWhite) return;

            selectedSquare = new int[]{row, col};
            legalDestinations = SimpleMoveGenerator.getLegalDestinations(board, row, col);
            redrawBoard();
            return;
        }

        boolean isLegalDestination = false;
        if (legalDestinations != null) {
            for (int[] m : legalDestinations) {
                if (m[0] == row && m[1] == col) { isLegalDestination = true; break; }
            }
        }

        if (isLegalDestination) {
            currentFen = BranchMoveApplier.applyMove(currentFen, selectedSquare[0], selectedSquare[1], row, col);
            lastMoveSquares = new int[]{selectedSquare[0], selectedSquare[1], row, col};
            selectedSquare = null;
            legalDestinations = null;
            redrawBoard();
            updateStatus();
            triggerAiMove();
        } else {
            char piece = board[row][col];
            if (piece != '.' && Character.isUpperCase(piece) == playerIsWhite) {
                selectedSquare = new int[]{row, col};
                legalDestinations = SimpleMoveGenerator.getLegalDestinations(board, row, col);
            } else {
                selectedSquare = null;
                legalDestinations = null;
            }
            redrawBoard();
        }
    }

    private void triggerAiMove() {
        aiThinking = true;
        statusText.setText("AI is thinking...");

        parser.reset();
        boolean isBlackToMove = currentFen.contains(" b ");
        parser.setSideToMove(isBlackToMove);

        engine.sendCommand("position fen " + currentFen);
        engine.sendCommand("go depth 12");
    }

    private void handleAiMoveResult(String bestmoveLine) {
        String[] tokens = bestmoveLine.split(" ");
        if (tokens.length < 2 || tokens[1].equals("(none)")) {
            aiThinking = false;
            statusText.setText("Game over");
            return;
        }

        String uciMove = tokens[1];
        int fromCol = uciMove.charAt(0) - 'a';
        int fromRow = 8 - (uciMove.charAt(1) - '0');
        int toCol = uciMove.charAt(2) - 'a';
        int toRow = 8 - (uciMove.charAt(3) - '0');

        currentFen = BranchMoveApplier.applyMove(currentFen, fromRow, fromCol, toRow, toCol);
        lastMoveSquares = new int[]{fromRow, fromCol, toRow, toCol};

        aiThinking = false;
        redrawBoard();
        updateStatus();
    }

    private void redrawBoard() {
        boardView.renderBranch(currentFen, selectedSquare, legalDestinations, lastMoveSquares);
    }

    private void updateStatus() {
        boolean whiteToMove = currentFen.contains(" w ");
        if (!gameActive) {
            statusText.setText("Pumili ng kulay para magsimula");
        } else if (whiteToMove == playerIsWhite) {
            statusText.setText("Your move");
        } else {
            statusText.setText("AI is thinking...");
        }
    }

    private void updateEvalBar(double pawns) {
        double clamped = Math.max(-8, Math.min(8, pawns));
        double whitePercent = 50 + (clamped / 8.0) * 50;

        evalBarWhite.post(() -> {
            View parent = (View) evalBarWhite.getParent();
            int totalWidth = parent.getWidth();
            int targetWidth = (int) (totalWidth * whitePercent / 100.0);

            int startWidth = currentEvalBarWidth >= 0
                    ? currentEvalBarWidth
                    : evalBarWhite.getLayoutParams().width;

            if (evalBarAnimator != null) {
                evalBarAnimator.cancel();
            }

            evalBarAnimator = ValueAnimator.ofInt(startWidth, targetWidth);
            evalBarAnimator.setDuration(300);
            evalBarAnimator.addUpdateListener(animation -> {
                int animatedWidth = (int) animation.getAnimatedValue();
                FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) evalBarWhite.getLayoutParams();
                p.width = animatedWidth;
                evalBarWhite.setLayoutParams(p);
                currentEvalBarWidth = animatedWidth;
            });
            evalBarAnimator.start();
        });

        String sign = pawns >= 0 ? "+" : "";
        evalBarText.setText(sign + String.format("%.2f", pawns));

        FrameLayout.LayoutParams textParams = (FrameLayout.LayoutParams) evalBarText.getLayoutParams();
        if (pawns >= 0) {
            textParams.gravity = android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL;
            evalBarText.setTextColor(Color.parseColor("#000000"));
        } else {
            textParams.gravity = android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL;
            evalBarText.setTextColor(Color.parseColor("#FFFFFF"));
        }
        evalBarText.setLayoutParams(textParams);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (engine != null) engine.stop();
    }
}