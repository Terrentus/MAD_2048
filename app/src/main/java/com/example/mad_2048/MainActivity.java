package com.example.mad_2048;

import static android.text.Selection.moveDown;
import static android.text.Selection.moveRight;
import static android.text.Selection.moveUp;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Toast;
import android.util.DisplayMetrics;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private GridLayout gameGrid;
    private TextView scoreText, statusMessage;
    private Button redoButton, resetButton;
    private int[][] board;
    private int[][] previousBoard;
    private int size = 4; // Default size
    private int score = 0;
    private int previousScore = 0;
    private int target = 2048; // Default target
    private Random random = new Random();
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupGame();
        setupGestureDetector();
    }

    private void initializeViews() {
        gameGrid = findViewById(R.id.gameGrid);
        scoreText = findViewById(R.id.scoreText);
        statusMessage = findViewById(R.id.statusMessage);
        redoButton = findViewById(R.id.redoButton);
        resetButton = findViewById(R.id.resetButton);

        // Mode buttons
        findViewById(R.id.mode2048).setOnClickListener(v -> setMode(4, 2048));
        findViewById(R.id.mode4096).setOnClickListener(v -> setMode(5, 4096));
        findViewById(R.id.mode8192).setOnClickListener(v -> setMode(6, 8192));

        redoButton.setOnClickListener(v -> undo());
        resetButton.setOnClickListener(v -> resetGame());
    }


    private void setupGame() {
        board = new int[size][size];
        previousBoard = new int[size][size];
        gameGrid.removeAllViews();
        gameGrid.setColumnCount(size);
        gameGrid.setRowCount(size);

        // Calculate the screen width and height
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        // Calculate the grid size (use the smaller of width or available height)
        int gridPadding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                32, // 16dp padding on each side
                getResources().getDisplayMetrics()
        );

        // Calculate available height (consider the space taken by other views)
        int availableHeight = screenHeight - gridPadding;
        // Use minimum of width or height to ensure square grid
        int gridSize = Math.min(screenWidth - gridPadding, availableHeight);
        int cellSize = (gridSize - gridPadding) / size;

        // Set grid size
        android.view.ViewGroup.LayoutParams gridParams = gameGrid.getLayoutParams();
        gridParams.width = gridSize;
        gridParams.height = gridSize;
        gameGrid.setLayoutParams(gridParams);

        // Calculate text size based on cell size
        float textSize = cellSize / 3.5f;
        if (size > 4) {
            textSize = cellSize / 4.0f;
        }

        // Create grid cells
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                TextView cell = new TextView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;                            
                params.height = cellSize;
                params.columnSpec = GridLayout.spec(j, 1f);
                params.rowSpec = GridLayout.spec(i, 1f);
                params.setMargins(4, 4, 4, 4);

                cell.setLayoutParams(params);
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                cell.setBackgroundColor(0xFFCDC1B4);
                cell.setTextColor(0xFF776E65);
                cell.setTypeface(cell.getTypeface(), Typeface.BOLD);

                gameGrid.addView(cell);
            }
        }

        // Initialize game state
        score = 0;
        scoreText.setText("0");
        statusMessage.setText("");

        addNewTile();
        addNewTile();
        updateUI();
    }

    private void setMode(int newSize, int newTarget) {
        // Save current state
        int oldSize = size;
        int oldTarget = target;
        int[][] oldBoard = board;
        int[][] oldPreviousBoard = previousBoard;
        int oldScore = score;

        try {
            // Update game parameters
            size = newSize;
            target = newTarget;

            // Allocate new arrays
            board = new int[size][size];
            previousBoard = new int[size][size];

            // Reset score and status
            score = 0;
            statusMessage.setText("");

            // Setup new game board
            setupGame();

        } catch (Exception e) {
            // Restore previous state if something goes wrong
            size = oldSize;
            target = oldTarget;
            board = oldBoard;
            previousBoard = oldPreviousBoard;
            score = oldScore;

            // Show error message
            Toast.makeText(this,
                    "Failed to switch mode. Please try again.",
                    Toast.LENGTH_SHORT).show();

            // Try to restore previous UI state
            try {
                setupGame();
            } catch (Exception ex) {
                // If restoration fails, show error and close activity
                Toast.makeText(this,
                        "Fatal error occurred. Please restart the app.",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }

        // Update UI elements for new mode
        String modeText = "";
        switch (target) {
            case 2048:
                modeText = "2048 Mode (4x4)";
                break;
            case 4096:
                modeText = "4096 Mode (5x5)";
                break;
            case 8192:
                modeText = "8192 Mode (6x6)";
                break;
        }

        // Show current mode in a toast message
        Toast.makeText(this,
                "Switched to " + modeText,
                Toast.LENGTH_SHORT).show();
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();

                if (Math.abs(dx) > Math.abs(dy)) {
                    if (dx > 0) move(Direction.RIGHT);
                    else move(Direction.LEFT);
                } else {
                    if (dy > 0) move(Direction.DOWN);
                    else move(Direction.UP);
                }
                return true;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private enum Direction { UP, DOWN, LEFT, RIGHT }

    private void move(Direction direction) {
        savePreviousState();
        boolean moved = false;

        switch (direction) {
            case UP:
                moved = moveUp();
                break;
            case DOWN:
                moved = moveDown();
                break;
            case LEFT:
                moved = moveLeft();
                break;
            case RIGHT:
                moved = moveRight();
                break;
        }

        if (moved) {
            addNewTile();
            updateUI();
            checkGameStatus();
        }
    }

    private boolean moveLeft() {
        boolean moved = false;
        for (int i = 0; i < size; i++) {
            int merge = 0;
            for (int j = 1; j < size; j++) {
                if (board[i][j] != 0) {
                    int column = j;
                    while (column > merge && board[i][column - 1] == 0) {
                        board[i][column - 1] = board[i][column];
                        board[i][column] = 0;
                        column--;
                        moved = true;
                    }
                    if (column > merge && board[i][column - 1] == board[i][column]) {
                        board[i][column - 1] *= 2;
                        score += board[i][column - 1];
                        board[i][column] = 0;
                        merge = column;
                        moved = true;
                    }
                }
            }
        }
        return moved;
    }

    private boolean moveRight() {
        boolean moved = false;
        for (int i = 0; i < size; i++) {
            int merge = size - 1;
            for (int j = size - 2; j >= 0; j--) {
                if (board[i][j] != 0) {
                    int column = j;
                    while (column < merge && board[i][column + 1] == 0) {
                        board[i][column + 1] = board[i][column];
                        board[i][column] = 0;
                        column++;
                        moved = true;
                    }
                    if (column < merge && board[i][column + 1] == board[i][column]) {
                        board[i][column + 1] *= 2;
                        score += board[i][column + 1];
                        board[i][column] = 0;
                        merge = column;
                        moved = true;
                    }
                }
            }
        }
        return moved;
    }

    private boolean moveUp() {
        boolean moved = false;
        for (int j = 0; j < size; j++) {
            int merge = 0;
            for (int i = 1; i < size; i++) {
                if (board[i][j] != 0) {
                    int row = i;
                    while (row > merge && board[row - 1][j] == 0) {
                        board[row - 1][j] = board[row][j];
                        board[row][j] = 0;
                        row--;
                        moved = true;
                    }
                    if (row > merge && board[row - 1][j] == board[row][j]) {
                        board[row - 1][j] *= 2;
                        score += board[row - 1][j];
                        board[row][j] = 0;
                        merge = row;
                        moved = true;
                    }
                }
            }
        }
        return moved;
    }

    private boolean moveDown() {
        boolean moved = false;
        for (int j = 0; j < size; j++) {
            int merge = size - 1;
            for (int i = size - 2; i >= 0; i--) {
                if (board[i][j] != 0) {
                    int row = i;
                    while (row < merge && board[row + 1][j] == 0) {
                        board[row + 1][j] = board[row][j];
                        board[row][j] = 0;
                        row++;
                        moved = true;
                    }
                    if (row < merge && board[row + 1][j] == board[row][j]) {
                        board[row + 1][j] *= 2;
                        score += board[row + 1][j];
                        board[row][j] = 0;
                        merge = row;
                        moved = true;
                    }
                }
            }
        }
        return moved;
    }

    // Similar implementation for moveRight, moveUp, moveDown
    // (omitted for brevity - implement using same logic as moveLeft with appropriate index modifications)

    private void savePreviousState() {
        previousScore = score;
        for (int i = 0; i < size; i++) {
            System.arraycopy(board[i], 0, previousBoard[i], 0, size);
        }
    }

    private void undo() {
        score = previousScore;
        for (int i = 0; i < size; i++) {
            System.arraycopy(previousBoard[i], 0, board[i], 0, size);
        }
        updateUI();
    }

    private void addNewTile() {
        ArrayList<Integer> emptyCells = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == 0) {
                    emptyCells.add(i * size + j);
                }
            }
        }

        if (!emptyCells.isEmpty()) {
            int randomCell = emptyCells.get(random.nextInt(emptyCells.size()));
            int row = randomCell / size;
            int col = randomCell % size;
            board[row][col] = (random.nextInt(10) < 9) ? 2 : 4;
        }
    }

    private void updateUI() {
        scoreText.setText(String.valueOf(score));
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                TextView cell = (TextView) gameGrid.getChildAt(i * size + j);
                int value = board[i][j];
                cell.setText(value > 0 ? String.valueOf(value) : "");
                cell.setBackgroundColor(getTileColor(value));
            }
        }
    }

    private int getTileColor(int value) {
        switch (value) {
            case 2: return 0xFFEEE4DA;
            case 4: return 0xFFEDE0C8;
            case 8: return 0xFFF2B179;
            case 16: return 0xFFF59563;
            case 32: return 0xFFF67C5F;
            case 64: return 0xFFF65E3B;
            case 128: return 0xFFEDCF72;
            case 256: return 0xFFEDCC61;
            case 512: return 0xFFEDC850;
            case 1024: return 0xFFEDC53F;
            case 2048: return 0xFFEDC22E;
            case 4096: return 0xFFEDC22E;
            case 8192: return 0xFFEDC22E;
            default: return 0xFFCDC1B4;
        }
    }

    private void checkGameStatus() {
        // Check for win
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == target) {
                    statusMessage.setText("You Won!");
                    return;
                }
            }
        }

        // Check for available moves
        if (!hasAvailableMoves()) {
            statusMessage.setText("Game Over :(");
        }
    }

    private boolean hasAvailableMoves() {
        // Check for empty cells
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == 0) return true;
            }
        }

        // Check for possible merges
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size - 1; j++) {
                if (board[i][j] == board[i][j + 1]) return true;
                if (board[j][i] == board[j + 1][i]) return true;
            }
        }
        return false;
    }

    private void resetGame() {
        score = 0;
        previousScore = 0;
        statusMessage.setText("");
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                board[i][j] = 0;
                previousBoard[i][j] = 0;
            }
        }
        setupGame();
    }
}