package net.benjaminurquhart.tilepuzzlegen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
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
	 */
	
	public static void main(String[] args) throws Exception {
		TileMaze maze = TileMaze.parse(
	 			"RGY.GP.YGRY.RG.OYRRORY.YYGGBYBOGGGRGGGR",
				"GOPBORRBYRRBBBRRR..RB.GRPOBOPP.BRGRGRGR",
				".R..BBROR.BPR.YGPOPYPPRR.OBPPBY.RGRGRGR",
				"GYBYR.GOPPO.RO.ORBB.OBGO.BPOYB..GGGGRGG"
		);
		
		ImageIO.write(solveAndRender(maze), "png", new File("maze-solved.png"));
	}
	
	public static BufferedImage solveAndRender(TileMaze maze) {
		long start = System.currentTimeMillis();
		List<Direction> solution = solve(maze);
		System.out.println(solution);
		System.out.printf("Finished in %dms\n", System.currentTimeMillis() - start);
		
		final int tileSize = 32;
		
		int width = maze.getWidth();
		int height = maze.getHeight();
		
		BufferedImage image = new BufferedImage(width * tileSize, height * tileSize, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		Tile tile;
		
		for(int x = 0; x < width; x++) {
			for(int y = 0; y < height; y++) {
				tile = maze.getTile(x, y);
				graphics.setColor(tile.getType().getColor());
				graphics.fillRect(x * tileSize, y * tileSize, tileSize, tileSize);
			}
		}
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
		
		graphics.dispose();
		return image;
	}
	
	
	public static List<Direction> solve(TileMaze maze) {
		
		Queue<TileMaze> queue = new PriorityQueue<>((a,b) -> b.getX() - a.getX());
		List<TileMaze> next = new ArrayList<>();
		TileMaze current, clone, best = null;
		Tile tile;
		
		Set<TileMaze> seen = new HashSet<>();
		
		queue.add(maze);
		seen.add(maze);
		int depth = 0;
		
		while(!queue.isEmpty()) {
			best = queue.peek();
			
			System.err.println("Reached depth of " + depth + " with " + queue.size() + " states");
			
			while(!queue.isEmpty()) {
				current = queue.poll();
				tile = current.getTile(current.getX(), current.getY());
				for(Direction direction : tile.getAvailableDirections(current.entry)) {
					clone = current.clone();
					clone.previous = current;
					clone.move = direction;
					
					if(clone.attemptMove(direction)) {
						if(!seen.add(clone)) {
							continue;
						}
						
						if(clone.getX() == clone.getWidth() - 1) {
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
		
		System.err.println("Failed to find a path.");
		System.err.println("Closest:");
		System.err.println(best);
		System.err.println(convertToList(best));
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
