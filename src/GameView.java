import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

public class GameView {
    private Canvas canvas;
    private GraphicsContext gc;
    private Stage stage;
    private GameController controller;

    public GameView(Stage stage, GameController controller) {
        this.stage = stage;
        this.controller = controller;
        this.canvas = new Canvas(GameConfig.WIDTH * GameConfig.TILE_SIZE, 
                               GameConfig.HEIGHT * GameConfig.TILE_SIZE);
        this.gc = canvas.getGraphicsContext2D();
    }

    public void initializeUI() {
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("🐍 Snake Game - Level " + (controller.getLevel()));
        stage.show();
    }

    public void render(int level, int score, List<Point> snake, Point food, List<Point> obstacles, boolean gameOver) {
        // Clear canvas
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, GameConfig.WIDTH * GameConfig.TILE_SIZE, 
                   GameConfig.HEIGHT * GameConfig.TILE_SIZE);

        // Draw grid
        gc.setStroke(Color.DARKGRAY);
        for (int i = 0; i <= GameConfig.WIDTH; i++) {
            gc.strokeLine(i * GameConfig.TILE_SIZE, 0, 
                         i * GameConfig.TILE_SIZE, GameConfig.HEIGHT * GameConfig.TILE_SIZE);
        }
        for (int i = 0; i <= GameConfig.HEIGHT; i++) {
            gc.strokeLine(0, i * GameConfig.TILE_SIZE, 
                         GameConfig.WIDTH * GameConfig.TILE_SIZE, i * GameConfig.TILE_SIZE);
        }

        // Draw obstacles
        gc.setFill(Color.GRAY);
        for (Point p : obstacles) {
            gc.fillRect(p.x * GameConfig.TILE_SIZE, p.y * GameConfig.TILE_SIZE, 
                       GameConfig.TILE_SIZE, GameConfig.TILE_SIZE);
        }

        // Draw food
        gc.setFill(Color.RED);
        gc.fillOval(food.x * GameConfig.TILE_SIZE, food.y * GameConfig.TILE_SIZE, 
                   GameConfig.TILE_SIZE, GameConfig.TILE_SIZE);

        // Draw snake
        for (int i = 0; i < snake.size(); i++) {
            Point p = snake.get(i);
            if (i == snake.size() - 1) {
                gc.setFill(Color.LIMEGREEN); // head
            } else {
                gc.setFill(Color.GREEN); // body
            }
            gc.fillRect(p.x * GameConfig.TILE_SIZE, p.y * GameConfig.TILE_SIZE, 
                       GameConfig.TILE_SIZE, GameConfig.TILE_SIZE);
        }

        // Draw score and level
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 18));
        gc.fillText("Score: " + score, 10, 20);
        gc.fillText("Level: " + level, GameConfig.WIDTH * GameConfig.TILE_SIZE - 90, 20);

        // Draw game over message
        if (gameOver) {
            gc.setFill(Color.RED);
            gc.setFont(Font.font("Arial", 36));
            gc.fillText("GAME OVER", 
                       GameConfig.WIDTH * GameConfig.TILE_SIZE / 2 - 100, 
                       GameConfig.HEIGHT * GameConfig.TILE_SIZE / 2);
        }
    }

    public void showGameOver(int score) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText("Game Over! Your score: " + score);
        alert.setContentText("Do you want to play again?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            controller.restartGame();
        } else {
            System.exit(0);
        }
    }

    public Canvas getCanvas() {
        return canvas;
    }
}