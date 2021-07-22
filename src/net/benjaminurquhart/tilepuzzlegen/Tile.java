package net.benjaminurquhart.tilepuzzlegen;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class Tile {

	public static enum Color {
		RED, // Wall
		YELLOW, // Shock, push back
		GREEN, // Fight monster, no-op
		ORANGE, // Set flavor to orange
		BLUE, // Water, push back if flavor is orange or adjacent to yellow
		PURPLE, // Slide to next tile, set flavor to lemon
		PINK; // no-op
		
		public static Color parse(int c) {
			String str = String.valueOf((char)c).toUpperCase();
			for(Color color : values()) {
				if(color.toString().equals(str)) {
					return color;
				}
			}
			return null;
		}
		
		public java.awt.Color getColor() {
			switch(this) {
			case RED: return java.awt.Color.RED;
			case ORANGE: return java.awt.Color.ORANGE;
			case YELLOW: return java.awt.Color.YELLOW;
			case GREEN: return java.awt.Color.GREEN;
			case BLUE: return java.awt.Color.BLUE;
			case PURPLE: return new java.awt.Color(0xff00ff);
			case PINK: return java.awt.Color.PINK;
			}
			return java.awt.Color.WHITE;
		}
		
		public String toString() {
			return this == PINK ? "." : String.valueOf(name().charAt(0));
		}
	}
	
	private int x, y;
	private Color type;
	private TileMaze maze;
	private Direction entryDirection;
	
	public Tile(TileMaze maze, Color type, int x, int y) {
		this.maze = maze;
		this.type = type;
		this.x = x;
		this.y = y;
	}
	
	protected void setType(Color type) {
		this.type = type;
	}
	
	public Color getType() {
		return type;
	}
	
	public Set<Direction> getAvailableDirections(Direction direction) {
		Set<Direction> out = computeAvailableDirections(direction);
		//System.err.printf("%s [%d, %d]: %s\n", type, x, y, out);
		return out;
	}
	
	private Set<Direction> computeAvailableDirections(Direction direction) {
		Direction entryDirection = this.entryDirection;
		if(entryDirection == null) {
			entryDirection = direction;
		}
		switch(type) {
		case RED: return Collections.emptySet();
		case YELLOW: return EnumSet.of(entryDirection.getOpposite());
		
		case PINK: 
		case GREEN: 
		case ORANGE: return EnumSet.allOf(Direction.class);
		
		case BLUE: {
			if(maze.getFlavor() == Flavor.ORANGE) {
				return Collections.emptySet();
			}
			
			Tile[] tiles = maze.getTilesAround(x, y);
			Set<Direction> dirs = new HashSet<>();
			Tile tile;

			for(int i = 0; i < 4; i++) {
				tile = tiles[i];
				
				if(tile != null) {
					if(tile.getType() == Color.YELLOW) {
						return Collections.emptySet();
					}
					else {
						dirs.add(Direction.values()[i]);
					}
				}
			}
			
			return dirs;
		}
		case PURPLE: return EnumSet.of(entryDirection);
		}
		
		
		return Collections.emptySet();
	}
	
	public boolean canMoveIntoFrom(Direction direction) {
		if(type == Color.BLUE && maze.getFlavor() == Flavor.ORANGE) {
			return false;
		}
		Tile other = maze.getTileRelative(x, y, direction.getOpposite());
		if(other.type == Color.PURPLE && direction != other.entryDirection) {
			return false;
		}
		return type != Color.RED; //getAvailableDirections(direction).contains(direction);
	}
	
	public boolean moveIntoFrom(Direction direction) {
		if(canMoveIntoFrom(direction)) {
			Tile other = maze.getTileRelative(x, y, direction.getOpposite());
			if(other.type == Color.PURPLE && direction != other.entryDirection) {
				throw new IllegalStateException(String.format(
						"\nIllegal movement: cannot move from %s (%d, %d) to %s (%d, %d), direction != entry (%s != %s)\nMoves: %s",
						other.type.name(), other.x, other.y, type.name(), x, y, direction, other.entryDirection, TilePuzzleSolver.convertToList(maze)
				));
			}
			maze.setPosition(x, y);
			entryDirection = direction;
			other.entryDirection = null;
			if(type == Color.PURPLE) {
				maze.setFlavor(Flavor.LEMON);
			}
			else if(type == Color.ORANGE) {
				maze.setFlavor(Flavor.ORANGE);
			}
			return true;
		}
		return false;
	}
	
	public Tile clone(TileMaze maze) {
		Tile tile = new Tile(maze, type, x, y);
		tile.entryDirection = entryDirection;
		//tile.directions = directions;
		
		return tile;
	}
	
	public String toString() {
		return type == null ? "?" : type.toString().toLowerCase();
	}
}
