package com.badlogic.gdx.maps.tiled;

import com.badlogic.gdx.maps.MapLayer;

/**
 * @brief Layer for a TiledMap 
 */
public class TiledMapTileLayer extends MapLayer {

	private int width;
	private int height;
	
	private float tileWidth;
	private float tileHeight;
	
	private Cell[][] cells;
	
	/**
	 * @return layer's width in tiles
	 */
	public int getWidth() {
		return width;
	}
	
	/**
	 * @return layer's height in tiles
	 */
	public int getHeight() {
		return height;
	}
	
	/**
	 * @return tiles' width in pixels
	 */
	public float getTileWidth() {
		return tileWidth;
	}
	
	/**
	 * @return tiles' height in pixels
	 */
	public float getTileHeight() {
		return tileHeight;
	}
	
	/**
	 * Creates TiledMap layer
	 * 
	 * @param width layer width in tiles
	 * @param height layer height in tiles
	 * @param tileWidth tile width in pixels
	 * @param tileHeight tile height in pixels 
	 */
	public TiledMapTileLayer(int width, int height, int tileWidth, int tileHeight) {
		super();
		this.width = width;
		this.height = height;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.cells = new Cell[width][height];
	}
	
	/**
     * Gets the {@link Cell} at the given coordinates.
     * Returns null if the cell is outside the bounds of the map.
	 * @param x 0-based X coordinate of the cell in the map.
	 * @param y 0-based Y coordinate of the cell in the map.
	 * @return cell at (x, y)
	 */
	public Cell getCell(int x, int y) {
		if(x < 0 || x >= width) return null;
		if(y < 0 || y >= height) return null;
		return cells[x][y];
	}
	
	/**
	 * Sets the {@link Cell} at the given coordinates.
	 * Does nothing if the cell is outside the bounds of the map.
     * @param x 0-based X coordinate of the cell in the map.
     * @param y 0-based Y coordinate of the cell in the map.
	 * @param cell
	 */
	public void setCell(int x, int y, Cell cell) {
		if(x < 0 || x >= width) return;
		if(y < 0 || y >= height) return;
		cells[x][y] = cell;
	}
	
	/**
	 * @brief represents a cell in a TiledLayer: TiledMapTile, flip and rotation properties.
	 */
	public static class Cell {
		
		private TiledMapTile tile;
		
		private boolean flipHorizontally;
		
		private boolean flipVertically;
		
		private int rotation;
		
		/**
		 * @return The tile currently assigned to this cell.
		 */
		public TiledMapTile getTile() {
			return tile;
		}
		
		/**
		 * Sets the tile to be used for this cell.
		 * 
		 * @param tile
		 */
		public void setTile(TiledMapTile tile) {
			this.tile = tile;
		}

		/**
		 * @return Whether the tile should be flipped horizontally.
		 */		
		public boolean getFlipHorizontally() {
			return flipHorizontally;
		}
		
		/**
		 * Sets whether to flip the tile horizontally.
		 * 
		 * @param flipHorizontally
		 */
		public void setFlipHorizontally(boolean flipHorizontally) {
			this.flipHorizontally = flipHorizontally;
		}
		
		/**
		 * @return Whether the tile should be flipped vertically.
		 */
		public boolean getFlipVertically() {
			return flipVertically;
		}
		
		/**
		 * Sets whether to flip the tile vertically.
		 * 
		 * @param flipVertically
		 */
		public void setFlipVertically(boolean flipVertically) {
			this.flipVertically = flipVertically;
		}
		
		/**
		 * @return The rotation of this cell, in degrees.
		 */
		public int getRotation() {
			return rotation;
		}
		
		/**
		 * Sets the rotation of this cell, in degrees.
		 * 
		 * @param rotation
		 */
		public void setRotation(int rotation) {
			this.rotation = rotation;
		}
		
		public static final int ROTATE_0 = 0;
		public static final int ROTATE_90 = 1;
		public static final int ROTATE_180 = 2;
		public static final int ROTATE_270 = 3;
		
	}
	
}
