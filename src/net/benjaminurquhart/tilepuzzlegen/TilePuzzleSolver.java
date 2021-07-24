package net.benjaminurquhart.tilepuzzlegen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import javax.imageio.ImageIO;

public class TilePuzzleSolver {
	
	/*
	 UT:
	 			"RGY.GP.YGRY.RG.OYRRORY.YYGGBYBOGGGRGGGR",
				"GOPBORRBYRRBBBRRR..RB.GRPOBOPP.BRGRGRGR",
				".R..BBROR.BPR.YGPOPYPPRR.OBPPBY.RGRGRGR",
				"GYBYR.GOPPO.RO.ORBB.OBGO.BPOYB..GGGGRGG"
	
	Troll:
				"RGG...YRBGYYRYO..PGBOOORYBYYBYBGGGRGGGR",
				".OBPBPGOB.PO.RPORRBB.R.GPOPOPOPBRGRGRGR",
				"GPR.RRR.PBGB.GBRYYBPYROR.P.YB...RGRGRGR",
				"RBBGPPBYRBBRO.ROOOO.YR.YRGBG.RR.GGGGRGG"
				
	Test Red Blocking:
				"RRR",
				"P..",
				".RR",
				".RR"
	 */
	
	public static boolean DEBUG = false, TEST = false;
	
	public static final int BLOCK_SIZE = 32;
	public static final int MAZE_LENGTH = 400, MAZE_HEIGHT = 4;
	
	private static final Tile.Color[] COLORS = Tile.Color.values();
	
	public static void main(String[] args) throws Exception {
		
		if(TEST) {
			test();
			return;
		}
		
		long start = System.currentTimeMillis();
		List<Direction> solution = null;
		TileMaze maze = null;
		
		final int REAL_MAZE_LENGTH = MAZE_LENGTH + 1;
		
		int currentLength = Math.min(BLOCK_SIZE, REAL_MAZE_LENGTH), prevLength = 0;
		
		Tile.Color[][] currentGrid = new Tile.Color[MAZE_HEIGHT][currentLength], 
					   partialGrid = new Tile.Color[MAZE_HEIGHT][currentLength], 
					   tmpGrid = new Tile.Color[MAZE_HEIGHT][0];
		
		TileMaze current, previous = null;
		
		boolean reachedEnd = false;
		int offset, index = 0;
		
		long totalAttempts = 0, partialAttempts = 0;
		
		try {
			while(!reachedEnd) {
				if(currentLength == REAL_MAZE_LENGTH) {
					reachedEnd = true;
				}
				partialAttempts = 0;
				
				do {
					fillRandomGrid(currentGrid, offset = previous == null ? 0 : 1);
					for(int y = 0; y < currentGrid.length; y++) {
						System.arraycopy(currentGrid[y], offset, partialGrid[y], index, currentGrid[y].length - offset);
					}
					current = new TileMaze(partialGrid);
					current.prepare();
					
					if(previous != null) {
						current.setPosition(previous.getX(), previous.getY());
						current.setFlavor(previous.getFlavor());
						current.entry = previous.entry;
					}
					solution = solve(current);
					partialAttempts++;
					totalAttempts++;
				} while(solution == null);
				
				System.err.printf("Found partial for %d (%d attempt(s), %d total)\n", currentLength, partialAttempts, totalAttempts);
				
				if(currentLength < REAL_MAZE_LENGTH) {
					prevLength = currentLength;
					
					currentLength += BLOCK_SIZE - offset;
					
					if(currentLength > REAL_MAZE_LENGTH) {
						currentLength = REAL_MAZE_LENGTH;
					}
					
					index += currentLength - prevLength;
					
					for(int y = 0; y < partialGrid.length; y++) {
						tmpGrid[y] = partialGrid[y];
						partialGrid[y] = new Tile.Color[currentLength];
						System.arraycopy(tmpGrid[y], 0, partialGrid[y], 0, tmpGrid[y].length);
						tmpGrid[y] = null;
					}
					
					for(int y = 0; y < currentGrid.length; y++) {
						currentGrid[y][0] = currentGrid[y][currentGrid[y].length - 1];
					}
				}
				else {
					break;
				}
				previous = current;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			System.err.println("partialGrid:");
			Arrays.stream(partialGrid).map(Arrays::toString).forEach(System.err::println);
			System.err.println("currentGrid:");
			Arrays.stream(currentGrid).map(Arrays::toString).forEach(System.err::println);
			return;
		}
		
		System.err.flush();
		System.out.flush();
		
		for(int y = 0; y < partialGrid.length; y++) {
			partialGrid[y] = Arrays.copyOfRange(partialGrid[y], 0, partialGrid[y].length - 1);
		}
		
		System.out.println("Found possible chained solution, testing...");
		long solveStart = System.currentTimeMillis();
		solution = solve(maze = new TileMaze(partialGrid));
		long solveTime = System.currentTimeMillis() - solveStart;
		if(solution == null) {
			System.out.printf("Nope (%dms)\n", solveTime);
			System.out.println(maze);
			ImageIO.write(render(maze, null), "png", new File("maze-failed.png"));
			return;
		}
		System.out.printf("Finished in %dms (verification took %dms)\n", System.currentTimeMillis() - start, solveTime);
		System.out.println(maze);
		System.out.println(solution);
		
		ImageIO.write(render(maze, solution), "png", new File("maze-solved.png"));
	}
	
	private static void test() throws Exception {
		TileMaze maze = TileMaze.parse(
				"RGG...YRBGYYRYO..PGBOOORYBYYBYBGGGRGGGR",
				".OBPBPGOB.PO.RPORRBB.R.GPOPOPOPBRGRGRGR",
				"GPR.RRR.PBGB.GBRYYBPYROR.P.YB...RGRGRGR",
				"RBBGPPBYRBBRO.ROOOO.YR.YRGBG.RR.GGGGRGG"
		);
		ImageIO.write(solveAndRender(maze), "png", new File("maze-solved.png"));
	}
	
	private static void fillRandomGrid(Tile.Color[][] grid, int offset) {
		for(int y = 0; y < grid.length; y++) {
			for(int x = offset; x < grid[y].length; x++) {
				grid[y][x] = COLORS[(int)(Math.random() * COLORS.length)];
			}
		}
	}
	
	public static BufferedImage render(TileMaze maze, List<Direction> solution) {
		
		final int tileSize = 32;
		
		int width = maze.getWidth();
		int height = maze.getHeight();
		
		BufferedImage image = new BufferedImage(width * tileSize, height * tileSize, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		Tile tile;
		
		for(int x = 0; x < width; x++) {
			for(int y = 0; y < height; y++) {
				tile = maze.getTile(x, y);
				graphics.setColor(tile == null || tile.getType() == null ? Color.WHITE : tile.getType().getColor());
				graphics.fillRect(x * tileSize, y * tileSize, tileSize, tileSize);
			}
		}
		if(solution != null) {
			graphics.setColor(Color.BLACK);
			int x = maze.getX();
			int y = maze.getY();
			int prevX = x;
			int prevY = y;
			
			for(Direction dir : solution) {
				switch(dir) {
				case UP: y--; break;
				case DOWN: y++; break;
				case LEFT: x--; break;
				case RIGHT: x++; break;
				}
				graphics.drawLine(
						prevX * tileSize + tileSize/2, 
						prevY * tileSize + tileSize/2, 
						x * tileSize + tileSize/2, 
						y * tileSize + tileSize/2
				);
				prevX = x;
				prevY = y;
			}	
		}

		
		graphics.dispose();
		return image;
	}
	
	public static BufferedImage solveAndRender(TileMaze maze) {
		return render(maze, solve(maze));
	}
	
	public static List<Direction> solve(TileMaze maze) {
		return solve(maze, new HashSet<>());
	}
	
	public static List<Direction> solve(TileMaze maze, Set<TileMaze> seen) {
		long start = System.currentTimeMillis();
		
		Queue<TileMaze> queue = new PriorityQueue<>((a,b) -> (b.getX() - b.delay) - (a.getX() + a.delay));
		List<TileMaze> next = new ArrayList<>();
		TileMaze current, clone, best = null;
		Tile tile, nextTile;
		
		queue.add(maze);
		seen.add(maze);
		int depth = 0;
		
		while(!queue.isEmpty()) {
			best = queue.peek();
			
			if(DEBUG) System.err.println("Reached depth of " + depth + " with " + queue.size() + " states");
			
			while(!queue.isEmpty()) {
				current = queue.poll();
				if(current.delay > 0) {
					next.add(current);
					current.delay--;
					continue;
				}
				tile = current.getTile();
				for(Direction direction : tile.getAvailableDirections(current.entry)) {
					clone = current.clone();
					clone.previous = current;
					clone.move = direction;
					
					if(clone.attemptMove(direction)) {
						if(!seen.add(clone)) {
							continue;
						}
						
						nextTile = clone.getTile();
						if(nextTile.getType() == Tile.Color.YELLOW) {
							clone.delay = 5;
						}
						else if(clone.getX() == clone.getWidth() - 1 && (nextTile.getType() == null || nextTile.getAvailableDirections(direction).contains(Direction.RIGHT))) {
							if(DEBUG) System.out.printf("Finished in %dms\n", System.currentTimeMillis() - start);
							return convertToList(clone);
						}
						
						next.add(clone);
					}
				}
			}
			queue.addAll(next);
			next.clear();
			depth++;
		}
		
		if(DEBUG) {
			System.err.println("Failed to find a path.");
			System.err.println("Closest:");
			System.err.println(best);
			System.err.println(convertToList(best));
			System.out.printf("Finished in %dms\n", System.currentTimeMillis() - start);
		}
		return null;
	}
	
	public static List<Direction> convertToList(TileMaze head) {
		List<Direction> out = new ArrayList<>();
		TileMaze t = head;
		
		while(t.move != null) {
			out.add(t.move);
			t = t.previous;
		}
		Collections.reverse(out);
		return out;
	}
}
