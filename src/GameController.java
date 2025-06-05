import javafx.scene.control.ChoiceDialog;
import javafx.stage.Stage;
import java.util.*;

public class GameController {
    private List<Point> snake;
    private Point food;
    private List<Point> obstacles;
    private int level;
    private int score;
    private GameView gameView;
    private boolean gameRunning;
    private static final int[][] DIRECTIONS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}}; // Up, Right, Down, Left
    private Direction currentDirection;
    private boolean isPaused;
    private int speed;
    private Random random;

    public enum Direction {
        UP, RIGHT, DOWN, LEFT
    }

    public GameController() {
        random = new Random();
    }

    public void startGame(Stage stage) {
        level = chooseLevel();
        if (level == 0) {
            System.exit(0);
        }

        initializeGame();
        gameView = new GameView(stage, this);
        gameView.initializeUI();
        updateView();

        if (level == 1 || level == 2 || level == 3) {
            startAutoPlay();
        }
    }

    private int chooseLevel() {
        List<String> choices = Arrays.asList("1", "2", "3");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("1", choices);
        dialog.setTitle("Choose Level");
        dialog.setHeaderText("Select game level:");
        dialog.setContentText("Level:");

        Optional<String> result = dialog.showAndWait();
        return result.map(Integer::parseInt).orElse(0);
    }

    private void initializeGame() {
        snake = new ArrayList<>();
        obstacles = new ArrayList<>();
        score = 0;
        gameRunning = true;
        isPaused = false;
        speed = level == 1 ? 200 : level == 2 ? 150 : level == 3 ? 100 : 300;
        currentDirection = Direction.RIGHT;

        setupSnake();
        spawnFood();
        generateObstacles();
    }

    private void setupSnake() {
        snake.clear();
        for (int i = 0; i < GameConfig.INITIAL_SNAKE_LENGTH; i++) {
            snake.add(new Point(10 + i, 10));
        }
    }

    private void spawnFood() {
        Point newFood;
        do {
            newFood = new Point(random.nextInt(GameConfig.WIDTH), random.nextInt(GameConfig.HEIGHT));
        } while (snake.contains(newFood) || obstacles.contains(newFood));
        food = newFood;
    }

    private void generateObstacles() {
        obstacles.clear();

        int totalBlocks = level * GameConfig.OBSTACLES_PER_LEVEL;
        int blocksCreated = 0;

        while (blocksCreated < totalBlocks) {
            int shapeSize = random.nextBoolean() ? 2 : 3;
            int shapeType = random.nextInt(3);
            int startX = random.nextInt(GameConfig.WIDTH);
            int startY = random.nextInt(GameConfig.HEIGHT);

            List<Point> newShape = new ArrayList<>();

            if (shapeType == 0) { // Horizontal
                if (startX + shapeSize > GameConfig.WIDTH) continue;
                for (int i = 0; i < shapeSize; i++) {
                    newShape.add(new Point(startX + i, startY));
                }
            } else if (shapeType == 1) { // Vertical
                if (startY + shapeSize > GameConfig.HEIGHT) continue;
                for (int i = 0; i < shapeSize; i++) {
                    newShape.add(new Point(startX, startY + i));
                }
            } else { // L-shape
                if (startX + 1 >= GameConfig.WIDTH || startY + 1 >= GameConfig.HEIGHT) continue;
                newShape.add(new Point(startX, startY));
                newShape.add(new Point(startX + 1, startY));
                newShape.add(new Point(startX, startY + 1));
                shapeSize = 3;
            }

            boolean overlaps = false;
            for (Point p : newShape) {
                if (snake.contains(p) || p.equals(food) || obstacles.contains(p)) {
                    overlaps = true;
                    break;
                }
            }
            if (overlaps) continue;

            obstacles.addAll(newShape);
            blocksCreated += shapeSize;
        }
    }

    private void startAutoPlay() {
        new Thread(() -> {
            while (gameRunning) {
                try {
                    Thread.sleep(speed);
                    if (!isPaused) {
                        switch (level) {
                            case 1:
                                moveSnakeBFS();
                                break;
                            case 2:
                                moveSnakeDFS();
                                break;
                            case 3:
                                moveSnakeAStar();
                                break;
                        }
                        updateView();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // BFS Algorithm for level 1
    private void moveSnakeBFS() {
        List<Point> path = findPathToFoodBFS();
        if (path.size() > 1) {
            Point nextMove = path.get(1);
            moveSnakeTo(nextMove);
        } else {
            moveRandomly();
        }
        checkGameStatus();
    }
    
    private List<Point> findPathToFoodBFS() {
        Point head = snake.get(snake.size() - 1);
        Queue<Point> queue = new LinkedList<>();
        Map<Point, Point> cameFrom = new HashMap<>();
        
        queue.add(head);
        cameFrom.put(head, null);

        while (!queue.isEmpty()) {
            Point current = queue.poll();

            if (current.equals(food)) {
                return reconstructPath(cameFrom, current);
            }

            List<int[]> dirs = getPrioritizedDirections(current);
            
            for (int[] direction : dirs) {
                Point neighbor = new Point(
                    current.x + direction[0],
                    current.y + direction[1]
                );
                
                if (isValidMove(neighbor) && !cameFrom.containsKey(neighbor)) {
                    queue.add(neighbor);
                    cameFrom.put(neighbor, current);
                }
            }
        }
        return Collections.emptyList();
    }

    // DFS Algorithm for level 2
    private void moveSnakeDFS() {
        Point head = snake.get(snake.size() - 1);
        if (tryMoveDirectlyToFood()) {
            return;
        }
        System.out.println("RUN DFS");
        List<Point> path = findPathToFoodDFS(head, new HashSet<>(), new HashMap<>(), 0, 50);
        if (path != null && path.size() > 1) {
            Point nextMove = path.get(1);
            moveSnakeTo(nextMove);
        } else {
            moveRandomly();
        }
        checkGameStatus();
    }
    private List<Point> findPathToFoodDFS(Point current, Set<Point> visited, Map<Point, Point> cameFrom, int depth, int maxDepth) {
        if (depth > maxDepth) {
            return null;
        }
        
        visited.add(current);
        
        if (current.equals(food)) {
            return reconstructPath(cameFrom, current);
        }
        
        List<int[]> dirs = getPrioritizedDirections(current);
        
        for (int[] dir : dirs) {
            Point neighbor = new Point(current.x + dir[0], current.y + dir[1]);
            
            if (!visited.contains(neighbor) && isValidMove(neighbor)) {
                cameFrom.put(neighbor, current);
                List<Point> path = findPathToFoodDFS(neighbor, visited, cameFrom, depth + 1, maxDepth);
                if (path != null) {
                    return path;
                }
            }
        }
        
        return null;
    }
    // A* Algorithm for level 3
    private void moveSnakeAStar() {
        List<Point> path = findPathToFoodAStar();
        if (path.size() > 1) {
            Point nextMove = path.get(1);
            moveSnakeTo(nextMove);
        } else {
            moveRandomly();
        }
        checkGameStatus();
    }

    private List<Point> findPathToFoodAStar() {
        Point head = snake.get(snake.size() - 1);
        Map<Point, Point> cameFrom = new HashMap<>();
        Map<Point, Integer> gScore = new HashMap<>();
        Map<Point, Integer> fScore = new HashMap<>();
        PriorityQueue<Point> openSet = new PriorityQueue<>(Comparator.comparingInt(p -> fScore.getOrDefault(p, Integer.MAX_VALUE)));
        gScore.put(head, 0);
        fScore.put(head, heuristic(head, food));
        openSet.add(head);
        cameFrom.put(head, null);

        while (!openSet.isEmpty()) {
            Point current = openSet.poll();

            if (current.equals(food)) {
                return reconstructPath(cameFrom, current);
            }

            for (int[] direction : DIRECTIONS) {
                Point neighbor = new Point(current.x + direction[0], current.y + direction[1]);

                if (!isValidMove(neighbor)) continue;

                int tentativeG = gScore.get(current) + 1;

                if (tentativeG < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeG);
                    fScore.put(neighbor, tentativeG + heuristic(neighbor, food));

                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }
        return Collections.emptyList(); 
    }

    private int heuristic(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    private boolean tryMoveDirectlyToFood() {
        Point head = snake.get(snake.size() - 1);
        int dx = Integer.compare(food.x, head.x);
        int dy = Integer.compare(food.y, head.y);
        if (dx != 0 && dy==0) { //
            Point next = new Point(head.x + dx, head.y);
            if (isValidMove(next)) {
                moveSnakeTo(next);
                return true;
            }
        }
        
        if (dy != 0 && dx == 0) { //
            Point next = new Point(head.x, head.y + dy);
            if (isValidMove(next)) {
                moveSnakeTo(next);
                return true;
            }
        }
        return false;
    }



    private List<int[]> getPrioritizedDirections(Point current) {
        List<int[]> dirs = new ArrayList<>(Arrays.asList(DIRECTIONS));
        
        dirs.sort((d1, d2) -> {
            Point p1 = new Point(current.x + d1[0], current.y + d1[1]);
            Point p2 = new Point(current.x + d2[0], current.y + d2[1]);
            int dist1 = Math.abs(p1.x - food.x) + Math.abs(p1.y - food.y);
            int dist2 = Math.abs(p2.x - food.x) + Math.abs(p2.y - food.y);
            return Integer.compare(dist1, dist2);
        });
        
        return dirs;
    }

    private List<Point> reconstructPath(Map<Point, Point> cameFrom, Point current) {
        List<Point> path = new ArrayList<>();
        while (current != null) {
            path.add(current);
            current = cameFrom.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    private boolean isValidMove(Point point) {
        if (point.x < 0 || point.x >= GameConfig.WIDTH || 
            point.y < 0 || point.y >= GameConfig.HEIGHT) {
            return false;
        }

        for (int i = 0; i < snake.size() - 1; i++) {
            if (snake.get(i).equals(point)) {
                return false;
            }
        }

        return !obstacles.contains(point);
    }

    private void moveRandomly() {
        Point head = snake.get(snake.size() - 1);
        List<Point> possibleMoves = new ArrayList<>();

        for (int[] direction : DIRECTIONS) {
            Point newPoint = new Point(head.x + direction[0], head.y + direction[1]);
            if (isValidMove(newPoint)) {
                possibleMoves.add(newPoint);
            }
        }

        if (!possibleMoves.isEmpty()) {
            Point nextMove = possibleMoves.get(random.nextInt(possibleMoves.size()));
            moveSnakeTo(nextMove);
        } else {
            gameRunning = false;
        }
    }

    private void moveSnakeTo(Point destination) {
        Point newHead = new Point(destination.x, destination.y);

        if (newHead.equals(food)) {
            snake.add(newHead);
            score += 10;
            spawnFood();
            speed = Math.max(50, speed - 5);
        } else {
            snake.add(newHead);
            snake.remove(0);
        }
    }

    private void checkGameStatus() {
        Point head = snake.get(snake.size() - 1);

        if (head.x < 0 || head.x >= GameConfig.WIDTH || 
            head.y < 0 || head.y >= GameConfig.HEIGHT) {
            gameOver();
            return;
        }

        for (int i = 0; i < snake.size() - 1; i++) {
            if (head.equals(snake.get(i))) {
                gameOver();
                return;
            }
        }

        if (obstacles.contains(head)) {
            gameOver();
        }
    }

    private void gameOver() {
        gameRunning = false;
        gameView.showGameOver(score);
    }

    public void togglePause() {
        isPaused = !isPaused;
    }

    public void restartGame() {
        initializeGame();
        updateView();
    }

    public void changeDirection(Direction newDirection) {
        if ((currentDirection == Direction.UP && newDirection != Direction.DOWN) ||
            (currentDirection == Direction.DOWN && newDirection != Direction.UP) ||
            (currentDirection == Direction.LEFT && newDirection != Direction.RIGHT) ||
            (currentDirection == Direction.RIGHT && newDirection != Direction.LEFT)) {
            currentDirection = newDirection;
        }
    }

    private void updateView() {
        gameView.render(level, score, snake, food, obstacles, !gameRunning);
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public int getScore() {
        return score;
    }

    public int getLevel() {
        return level;
    }
}