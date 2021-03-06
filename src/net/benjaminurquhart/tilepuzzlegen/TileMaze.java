package net.benjaminurquhart.tilepuzzlegen;

import net.benjaminurquhart.tilepuzzlegen.Tile.Color;

public class TileMaze {
	
	public static TileMaze parse(String... lines) {
		for(String s : lines) {
			if(s.length() != lines[0].length()) {
				System.err.println(String.join("\n", lines));
				System.err.printf("\n%s {} %s\n", s, lines[0]);
				throw new IllegalArgumentException("all lines must be the same length");
			}
		}
		Tile.Color[][] colors = new Tile.Color[lines.length][0];
		
		for(int i = 0; i < lines.length; i++) {
			colors[i] = lines[i].chars().mapToObj(Tile.Color::parse).toArray(Tile.Color[]::new);
		}
		
		return new TileMaze(colors);
	}
	public static TileMaze parse(String map) {
		return parse(map.replace("\r", "").split("\n"));
	}

	public Direction move;
	public Direction entry;
	public TileMaze previous;
	
	public int delay;
	
	private int width, height;
	private Flavor flavor;
	private Tile[][] grid;
	private int x, y;
	
	private TileMaze(int width, int height, Flavor flavor, int x, int y) {
		this.entry = Direction.RIGHT;
		
		if(x >= width) {
			x = width - 1;
		}
		if(x < 0) {
			x = 0;
		}
		
		if(y >= height) {
			y = height -1;
		}
		if(y < 0) {
			y = 0;
		}
		
		this.grid = new Tile[height][width];
		this.flavor = flavor;
		this.height = height;
		this.width = width;
		this.x = x;
		this.y = y;
	}
	
	public TileMaze(Tile.Color[][] grid) {
		this(grid[0].length, grid.length);
		
		for(int i = 0; i < height; i++) {
			for(int j = 0; j < width; j++) {
				this.grid[i][j].setType(grid[i][j]);
			}
		}
	}
	
	public TileMaze(int width, int height) {
		this(width, height, Flavor.NONE, 0, 2);
		for(int i = 0; i < height; i++) {
			for(int j = 0; j < width; j++) {
				grid[i][j] = new Tile(this, Tile.Color.PINK, j, i);
			}
		}
	}
	
	public Flavor getFlavor() {
		return flavor;
	}
	
	public void setFlavor(Flavor flavor) {
		this.flavor = flavor;
	}
	
	public Tile[] getTilesAround(int x, int y) {
		return new Tile[] {
			getTile(x, y - 1),
			getTile(x, y + 1),
			getTile(x - 1, y),
			getTile(x + 1, y)
		};
	}
	
	public Tile getTileRelative(Direction direction) {
		return getTileRelative(x, y, direction);
	}
	
	public Tile getTileRelative(int x, int y, Direction direction) {
		switch(direction) {
		case UP: return getTile(x, y - 1);
		case DOWN: return getTile(x, y + 1);
		case LEFT: return getTile(x - 1, y);
		case RIGHT: return getTile(x + 1, y);
		}
		return null;
	}
	
	public Tile getTile(int x, int y) {
		if(x < 0 || y < 0 || x >= width || y >= height) {
			return null;
		}
		return grid[y][x];
	}
	
	public Tile getTile() {
		return getTile(x, y);
	}
	
	public boolean attemptMove(Direction direction) {
		Tile destination = getTileRelative(direction);
		if(destination != null && destination.moveIntoFrom(direction)) {
			this.entry = direction;
			return true;
		}
		return false;
	}
	
	public void setPosition(int x, int y) {
		Tile tile = getTile(x, y);
		if(tile == null) {
			throw new IllegalArgumentException("position out of bounds: " + x + ", " + y);
		}
		this.x = x;
		this.y = y;
	}
	
	public void prepare() {
		Tile tile = getTile(x, y);
		if(tile.getType() == Color.PURPLE) {
			setFlavor(Flavor.LEMON);
		}
		else if(tile.getType() == Color.ORANGE) {
			setFlavor(Flavor.ORANGE);
		}
		previous = null;
		entry = Direction.RIGHT;
		move = null;
		
		delay = 0;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public TileMaze clone() {
		TileMaze maze = new TileMaze(width, height, flavor, x, y);
		for(int i = 0; i < height; i++) {
			for(int j = 0; j < width; j++) {
				maze.grid[i][j] = grid[i][j].clone(maze);
			}
		}
		maze.entry = entry;
		return maze;
	}
	
	public int hashCode() {
		return (x << 16) | (y << 8) | (flavor.ordinal() << 4) | entry.ordinal();
	}
	
	public boolean equals(Object other) {
		return (other instanceof TileMaze) && other != null && other.hashCode() == hashCode();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0; i < height; i++) {
			for(int j = 0; j < width; j++) {
				if(TilePuzzleSolver.DEBUG && j == x && i == y) {
					sb.append('X');
				}
				else {
					sb.append(grid[i][j]);
				}
			}
			sb.append('\n');
		}
		sb.append(String.format("%d x %d, pos=[%d, %d] (%s, %s)", width, height, x, y, getTile(x, y).getType().name(), flavor));
		return sb.toString();
	}
}
