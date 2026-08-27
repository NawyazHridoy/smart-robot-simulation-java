package com.example.smartrobotsimulations;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SmartRobotGUI extends Application {

    // Grid & UI Settings
    private final int gridSize = 6;
    private final int cellSize = 80;
    private Canvas canvas;
    private GraphicsContext gc;
    private Label scoreLabel, highScoreLabel, statusLabel, levelLabel, timeLabel, batteryText;
    private ProgressBar batteryBar;
    private BorderPane root;

    // Game States
    private int score = 0;
    private int highScore = 0;
    private int level = 1;
    private double battery = 1.0;
    private int timeLeft = 60;
    private boolean isGameOver = false;
    private boolean isDarkMode = false;
    private boolean shieldActive = false;
    private int shieldTimer = 0;
    private Timeline gameLoop;

    // Entities Coordinates
    private int robotX = 0, robotY = 5;
    private int enemyX = 5, enemyY = 5;
    private int dirtX = 2, dirtY = 2;
    private int chargeX = 5, chargeY = 0;
    private int powerUpX = -1, powerUpY = -1;

    // Bonus Item
    private int goldX = -1, goldY = -1;
    private int goldTimer = 0;

    // Patrol Bot
    private int patrolX = 3, patrolY = 3;
    private boolean patrolMovingRight = true;

    // Autonomous Pathfinding
    private List<int[]> autoPath = new ArrayList<>();
    private List<int[]> walls = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        loadHighScore();
        setupWalls();

        root = new BorderPane();
        root.setPadding(new Insets(20));

        // Canvas for Map
        canvas = new Canvas(gridSize * cellSize, gridSize * cellSize);
        gc = canvas.getGraphicsContext2D();
        canvas.setOnMouseClicked(this::handleMouseClick);

        VBox canvasContainer = new VBox(canvas);
        canvasContainer.setAlignment(Pos.CENTER);
        root.setCenter(canvasContainer);

        HBox topBar = createTopBar();
        root.setTop(topBar);

        VBox rightPanel = createRightPanel();
        root.setRight(rightPanel);

        applyTheme();
        drawGrid();
        startGameLoop();

        Scene scene = new Scene(root, 900, 650);

        // KEYBOARD CONTROLS (Always Active)
        scene.setOnKeyPressed(this::handleKeyPress);

        // Fix for keyboard focus loss when clicking around
        scene.setOnMouseClicked(e -> root.requestFocus());
        root.setFocusTraversable(true);
        root.requestFocus();

        primaryStage.setTitle("Smart Robot Pro - Landscape Edition");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setupWalls() {
        walls.clear();
        walls.add(new int[]{1, 2});
        walls.add(new int[]{1, 3});
        walls.add(new int[]{3, 1});
        walls.add(new int[]{2, 4});
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(30);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(0, 0, 20, 0));

        Button themeBtn = new Button("🌙 Dark Mode");
        themeBtn.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-background-radius: 20; -fx-font-weight: bold; -fx-padding: 8 15;");
        themeBtn.setFocusTraversable(false); // Prevents stealing keyboard arrow keys
        themeBtn.setOnAction(e -> {
            isDarkMode = !isDarkMode;
            themeBtn.setText(isDarkMode ? "☀️ Light Mode" : "🌙 Dark Mode");
            applyTheme();
            root.requestFocus();
        });

        statusLabel = new Label("System Ready! Use Arrows/WASD to drive.");
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        statusLabel.setTextFill(Color.RED);

        Button restartBtn = new Button("🔄 Restart");
        restartBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-background-radius: 20; -fx-font-weight: bold; -fx-padding: 8 15;");
        restartBtn.setFocusTraversable(false); // Prevents stealing keyboard arrow keys
        restartBtn.setOnAction(e -> {
            resetGame();
            root.requestFocus();
        });

        topBar.getChildren().addAll(themeBtn, statusLabel, restartBtn);
        return topBar;
    }

    private VBox createRightPanel() {
        VBox panel = new VBox(25);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(0, 20, 0, 40));

        levelLabel = new Label("Level: " + level);
        levelLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        levelLabel.setTextFill(Color.DODGERBLUE);

        scoreLabel = new Label("Score: " + score);
        scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        scoreLabel.setTextFill(Color.DARKORANGE);

        highScoreLabel = new Label("High Score: " + highScore);
        highScoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        highScoreLabel.setTextFill(Color.PURPLE);

        timeLabel = new Label("🕒 Time: " + timeLeft + "s");
        timeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        timeLabel.setTextFill(Color.CRIMSON);

        batteryText = new Label("Battery: 100%");
        batteryText.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        batteryBar = new ProgressBar(battery);
        batteryBar.setPrefWidth(200);
        batteryBar.setPrefHeight(25);
        batteryBar.setStyle("-fx-accent: #4CAF50; -fx-control-inner-background: #e0e0e0;");

        // D-Pad
        GridPane dPad = new GridPane();
        dPad.setAlignment(Pos.CENTER);
        dPad.setHgap(10);
        dPad.setVgap(10);

        String btnStyle = "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 15; -fx-min-width: 60; -fx-min-height: 60;";
        String centerStyle = "-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 20px; -fx-background-radius: 50; -fx-min-width: 60; -fx-min-height: 60;";

        Button btnUp = new Button("↑");
        Button btnDown = new Button("↓");
        Button btnLeft = new Button("←");
        Button btnRight = new Button("→");
        Button btnClean = new Button("🧹");

        // Disabling focus so arrow keys work seamlessly
        btnUp.setFocusTraversable(false);
        btnDown.setFocusTraversable(false);
        btnLeft.setFocusTraversable(false);
        btnRight.setFocusTraversable(false);
        btnClean.setFocusTraversable(false);

        btnUp.setStyle(btnStyle);
        btnDown.setStyle(btnStyle);
        btnLeft.setStyle(btnStyle);
        btnRight.setStyle(btnStyle);
        btnClean.setStyle(centerStyle);

        btnUp.setOnAction(e -> handleMove(0, -1));
        btnDown.setOnAction(e -> handleMove(0, 1));
        btnLeft.setOnAction(e -> handleMove(-1, 0));
        btnRight.setOnAction(e -> handleMove(1, 0));
        btnClean.setOnAction(e -> handleClean());

        dPad.add(btnUp, 1, 0);
        dPad.add(btnLeft, 0, 1);
        dPad.add(btnClean, 1, 1);
        dPad.add(btnRight, 2, 1);
        dPad.add(btnDown, 1, 2);

        panel.getChildren().addAll(levelLabel, scoreLabel, highScoreLabel, timeLabel, batteryText, batteryBar, dPad);
        return panel;
    }

    private void applyTheme() {
        if (isDarkMode) {
            root.setStyle("-fx-background-color: #2c3e50;");
            batteryText.setTextFill(Color.WHITE);
        } else {
            root.setStyle("-fx-background-color: #f5f6fa;");
            batteryText.setTextFill(Color.BLACK);
        }
        drawGrid();
    }

    // --- KEYBOARD CONTROLS ---
    private void handleKeyPress(KeyEvent event) {
        if (isGameOver) return;
        KeyCode code = event.getCode();
        switch (code) {
            case W: case UP: handleMove(0, -1); break;
            case S: case DOWN: handleMove(0, 1); break;
            case A: case LEFT: handleMove(-1, 0); break;
            case D: case RIGHT: handleMove(1, 0); break;
            case SPACE: case C: handleClean(); break;
            default: break;
        }
    }

    // --- GAME LOOP ---
    private void startGameLoop() {
        if (gameLoop != null) gameLoop.stop();
        gameLoop = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (isGameOver) return;

            timeLeft--;
            timeLabel.setText("🕒 Time: " + timeLeft + "s");

            if (timeLeft <= 0) {
                triggerGameOver("Time's Up!");
                return;
            }

            moveSmartEnemy();
            movePatrolBot();

            if (!autoPath.isEmpty()) {
                int[] nextStep = autoPath.remove(0);
                handleMove(nextStep[0] - robotX, nextStep[1] - robotY);
            }

            if (shieldActive) {
                shieldTimer--;
                if (shieldTimer <= 0) shieldActive = false;
            }

            if (goldTimer > 0) {
                goldTimer--;
                if (goldTimer == 0) {
                    goldX = -1; goldY = -1;
                }
            } else if (Math.random() < 0.10) {
                spawnGoldenDirt();
            }

            if (!shieldActive && powerUpX == -1 && Math.random() < 0.05) {
                powerUpX = (int) (Math.random() * gridSize);
                powerUpY = (int) (Math.random() * gridSize);
            }

            checkCollisions();
            updateUI();
            drawGrid();
        }));
        gameLoop.setCycleCount(Timeline.INDEFINITE);
        gameLoop.play();
    }

    // --- NEW SMART ENEMY AI (BFS Pathfinding) ---
    private void moveSmartEnemy() {
        int[] nextStep = getNextStepBFS(enemyX, enemyY, robotX, robotY);
        if (nextStep != null) {
            enemyX = nextStep[0];
            enemyY = nextStep[1];
        }
    }

    private int[] getNextStepBFS(int startX, int startY, int targetX, int targetY) {
        Queue<Node> queue = new LinkedList<>();
        boolean[][] visited = new boolean[gridSize][gridSize];
        queue.add(new Node(startX, startY, null));
        visited[startX][startY] = true;

        Node targetNode = null;
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            if (current.x == targetX && current.y == targetY) {
                targetNode = current;
                break;
            }

            for (int[] d : dirs) {
                int nx = current.x + d[0];
                int ny = current.y + d[1];
                if (isValid(nx, ny) && !visited[nx][ny]) {
                    visited[nx][ny] = true;
                    queue.add(new Node(nx, ny, current));
                }
            }
        }

        if (targetNode != null) {
            Node temp = targetNode;
            while (temp.parent != null && temp.parent.parent != null) {
                temp = temp.parent;
            }
            if (temp.parent != null) {
                return new int[]{temp.x, temp.y};
            }
        }
        return null;
    }

    // --- PATROL BOT LOGIC ---
    private void movePatrolBot() {
        if (patrolMovingRight) {
            if (isValid(patrolX + 1, patrolY)) patrolX++;
            else patrolMovingRight = false;
        } else {
            if (isValid(patrolX - 1, patrolY)) patrolX--;
            else patrolMovingRight = true;
        }
    }

    // --- USER CONTROLS & LOGIC ---
    private void handleMove(int dx, int dy) {
        if (isGameOver) return;

        int nx = robotX + dx;
        int ny = robotY + dy;

        if (isValid(nx, ny)) {
            robotX = nx;
            robotY = ny;

            if (!shieldActive) battery -= 0.05;

            if (robotX == chargeX && robotY == chargeY) {
                battery = 1.0;
                statusLabel.setText("Battery Fully Charged!");
            }

            if (robotX == powerUpX && robotY == powerUpY) {
                shieldActive = true;
                shieldTimer = 10;
                powerUpX = -1;
                powerUpY = -1;
                statusLabel.setText("Shield Active! (10s)");
            }

            checkCollisions();
            updateUI();
            drawGrid();
        } else {
            statusLabel.setText("💥 Hit a wall!");
            autoPath.clear();
        }
    }

    private void handleClean() {
        if (robotX == goldX && robotY == goldY) {
            score += 25;
            goldX = -1; goldY = -1; goldTimer = 0;
            statusLabel.setText("Golden Dirt Cleaned! +25 Points");
            checkLevelUp();
        } else if (robotX == dirtX && robotY == dirtY) {
            score += 10;
            statusLabel.setText("Dirt Cleaned! +10 Points");
            spawnNewDirt();
            checkLevelUp();
        }
        updateUI();
        drawGrid();
    }

    private void checkLevelUp() {
        if (score >= 50 && level == 1) {
            level = 2;
            walls.add(new int[]{4, 4});
            walls.add(new int[]{4, 5});
            statusLabel.setText("Level 2 Reached! More walls added.");
            timeLeft += 30; // Bonus time
        }
    }

    private void checkCollisions() {
        if (battery <= 0) {
            triggerGameOver("Battery Empty!");
        } else if (!shieldActive && robotX == enemyX && robotY == enemyY) {
            triggerGameOver("Caught by Enemy Robot!");
        } else if (!shieldActive && robotX == patrolX && robotY == patrolY) {
            triggerGameOver("Hit by Patrol Bot!");
        }
    }

    private void triggerGameOver(String reason) {
        isGameOver = true;
        gameLoop.stop();
        statusLabel.setText("☠️ Game Over: " + reason);
        if (score > highScore) {
            highScore = score;
            saveHighScore();
            highScoreLabel.setText("High Score: " + highScore);
        }
    }

    private void resetGame() {
        score = 0;
        level = 1;
        battery = 1.0;
        timeLeft = 60;
        isGameOver = false;
        shieldActive = false;
        robotX = 0; robotY = 5;
        enemyX = 5; enemyY = 5;
        patrolX = 3; patrolY = 3;
        autoPath.clear();
        setupWalls();
        spawnNewDirt();
        statusLabel.setText("System Ready! Use Arrows/WASD to drive.");
        updateUI();
        startGameLoop();
    }

    // --- AUTONOMOUS PATHFINDING (BFS) ---
    private void handleMouseClick(MouseEvent event) {
        if (isGameOver) return;
        int targetX = (int) (event.getX() / cellSize);
        int targetY = (int) (event.getY() / cellSize);
        calculateBFS(targetX, targetY);
    }

    private void calculateBFS(int tx, int ty) {
        if (!isValid(tx, ty)) return;
        Queue<Node> queue = new LinkedList<>();
        boolean[][] visited = new boolean[gridSize][gridSize];
        queue.add(new Node(robotX, robotY, null));
        visited[robotX][robotY] = true;

        Node targetNode = null;
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (current.x == tx && current.y == ty) {
                targetNode = current;
                break;
            }
            for (int[] d : dirs) {
                int nx = current.x + d[0];
                int ny = current.y + d[1];
                if (isValid(nx, ny) && !visited[nx][ny] && !(nx == patrolX && ny == patrolY)) {
                    visited[nx][ny] = true;
                    queue.add(new Node(nx, ny, current));
                }
            }
        }

        if (targetNode != null) {
            autoPath.clear();
            Node temp = targetNode;
            while (temp.parent != null) {
                autoPath.add(0, new int[]{temp.x, temp.y});
                temp = temp.parent;
            }
            statusLabel.setText("Auto-Pilot Engaged!");
        } else {
            statusLabel.setText("No path found to target.");
        }
    }

    // --- HELPERS & DRAWING ---
    private boolean isValid(int x, int y) {
        if (x < 0 || x >= gridSize || y < 0 || y >= gridSize) return false;
        for (int[] w : walls) {
            if (w[0] == x && w[1] == y) return false;
        }
        return true;
    }

    private void spawnNewDirt() {
        do {
            dirtX = (int) (Math.random() * gridSize);
            dirtY = (int) (Math.random() * gridSize);
        } while (!isValid(dirtX, dirtY) || (dirtX == chargeX && dirtY == chargeY));
    }

    private void spawnGoldenDirt() {
        do {
            goldX = (int) (Math.random() * gridSize);
            goldY = (int) (Math.random() * gridSize);
        } while (!isValid(goldX, goldY));
        goldTimer = 5;
    }

    private void drawGrid() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        int gap = 4;
        int drawSize = cellSize - gap;

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                int px = (i * cellSize) + (gap / 2);
                int py = (j * cellSize) + (gap / 2);

                gc.setFill(isDarkMode ? Color.rgb(60, 60, 60) : Color.LIGHTGRAY);
                gc.fillRect(px, py, drawSize, drawSize);

                for (int[] w : walls) {
                    if (w[0] == i && w[1] == j) {
                        gc.setFill(Color.DARKRED);
                        gc.fillRect(px, py, drawSize, drawSize);
                    }
                }

                if (i == chargeX && j == chargeY) {
                    gc.setFill(Color.GOLD);
                    gc.fillRect(px, py, drawSize, drawSize);
                }
                if (i == dirtX && j == dirtY) {
                    gc.setFill(Color.SADDLEBROWN);
                    gc.fillRect(px, py, drawSize, drawSize);
                }
                if (i == powerUpX && j == powerUpY) {
                    gc.setFill(Color.CYAN);
                    gc.fillRect(px + 10, py + 10, drawSize - 20, drawSize - 20);
                }
                if (i == goldX && j == goldY) {
                    gc.setFill(Color.ORANGE);
                    gc.fillRect(px + 10, py + 10, drawSize - 20, drawSize - 20);
                }

                if (i == patrolX && j == patrolY) {
                    gc.setFill(Color.CRIMSON);
                    gc.fillOval(px + 10, py + 10, drawSize - 20, drawSize - 20);
                }

                if (i == enemyX && j == enemyY) {
                    gc.setFill(Color.BLACK);
                    gc.fillOval(px + 5, py + 5, drawSize - 10, drawSize - 10);
                }

                if (i == robotX && j == robotY) {
                    gc.setFill(shieldActive ? Color.CYAN : Color.DODGERBLUE);
                    gc.fillOval(px + 5, py + 5, drawSize - 10, drawSize - 10);
                }
            }
        }
    }

    private void updateUI() {
        scoreLabel.setText("Score: " + score);
        levelLabel.setText("Level: " + level);
        batteryBar.setProgress(battery);
        batteryText.setText("Battery: " + (int)(battery * 100) + "%");

        if (battery > 0.5) batteryBar.setStyle("-fx-accent: #4CAF50;");
        else if (battery > 0.2) batteryBar.setStyle("-fx-accent: orange;");
        else batteryBar.setStyle("-fx-accent: red;");
    }

    private void loadHighScore() {
        try {
            File file = new File("highscore.txt");
            if (file.exists()) {
                Scanner scanner = new Scanner(file);
                if (scanner.hasNextInt()) highScore = scanner.nextInt();
                scanner.close();
            }
        } catch (IOException e) { }
    }

    private void saveHighScore() {
        try {
            FileWriter writer = new FileWriter("highscore.txt");
            writer.write(String.valueOf(highScore));
            writer.close();
        } catch (IOException e) { }
    }

    private static class Node {
        int x, y;
        Node parent;
        Node(int x, int y, Node parent) {
            this.x = x; this.y = y; this.parent = parent;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}