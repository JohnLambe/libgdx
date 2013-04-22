package com.badlogic.gdx.maps.objects;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.MathUtils;

/**
 * @brief Represents a map object containing a texture (region).
 * 
 * Uses of this include Tile map objects (objects placed with the 'Insert Tile' tool in Tiled),
 * and textures in a non-tiled map.
 * 
 * @see See the comments on {@link com.badlogic.gdx.maps.tiled.TmxMapLoader}
 *      for what properties it populates on this object.
 */
public class TextureMapObject extends MapObject {
	/** (Not available)
	 * Value of the width or height properties if they are not available,
	 * because the texture region is not set. */
	public static final float NA = -1.0f;

	/** @see #getX() */
	private float x = 0.0f;
	/** @see #getY() */
	private float y = 0.0f;
	/** @see #getOriginX() */
	private float originX = 0.0f;
	/** @see #getOriginY() */
	private float originY = 0.0f;
	/** @see #getScaleX() */
	private float scaleX = 1.0f;
	/** @see #getScaleY() */
	private float scaleY = 1.0f;
	/** @see #getRotation() */
	private float rotation = 0.0f;
	/** @see #getTextureRegion() */
	private TextureRegion textureRegion = null;

	/**
	 * @return x axis coordinate
	 */
	public float getX() {
		return x;
	}

	/**
	 * @param x new x axis coordinate
	 */
	public void setX(float x) {
		this.x = x;
	}

	/**
	 * @return y axis coordinate
	 */
	public float getY() {
		return y;
	}

	/**
	 * @param y new y axis coordinate
	 */
	public void setY(float y) {
		this.y = y;
	}

	/**
	 * Set the coordinates of the corner of the object (the same as calling {@link #setX} and {@link #setY}).
	 * @param x new x coordinate
	 * @param y new y coordinate
	 */
	public void setCoordinates(float x, float y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * @return x axis origin (the origin relative to the left of the object in map units)
	 */
	public float getOriginX() {
		return originX;
	}

	/**
	 * @param x new x axis origin (the origin relative to the left of the object in map units)
	 * @see #setOrigin(float, float)
	 */
	public void setOriginX(float x) {
		this.originX = x;
	}

	/**
	 * @return y axis origin
	 */
	public float getOriginY() {
		return originY;
	}

	/**
	 * @param y new axis origin
	 * @see #setOrigin(float, float)
	 */
	public void setOriginY(float y) {
		this.originY = y;
	}

	/**
	 * Set the origin (the same as {@link #setOriginX} and {@link #setOriginY}) -
	 * the point about which the texture is rotated or from which it is scaled.
	 * (Relative to the corner of the object.)
	 * @param x x coordinate of origin
	 * @param y y coordinate of origin
	 */
	public void setOrigin(float x, float y) {
		this.originX = x;
		this.originY = y;
	}

	/**
	 * @return x axis scale
	 */
	public float getScaleX() {
		return scaleX;
	}

	/**
	 * @param x new x axis scale 
	 */
	public void setScaleX(float x) {
		this.scaleX = x;
	}

	/**
	 * @return y axis scale
	 */
	public float getScaleY() {
		return scaleY;
	}

	/**
	 * @param y new y axis scale
	 */
	public void setScaleY(float y) {
		this.scaleY = y;
	}

	/**
	 * @return texture's rotation in radians, clockwise
	 */
	public float getRotation() {
		return rotation;
	}

	/**
	 * @param rotation texture's new rotation in radians, clockwise
	 */
	public void setRotation(float rotation) {
		this.rotation = rotation;
	}
	
	/** The rotation in degrees. */
	public float getRotationDegrees() {
		return this.rotation * MathUtils.radiansToDegrees;
	}
	
	/**
	 * @param rotation texture's new rotation in degrees
	 */
	public void setRotationDegrees(float rotationDegrees) {
		this.rotation = rotationDegrees * MathUtils.degreesToRadians;
	}

	/**
	 * @return region
	 */
	public TextureRegion getTextureRegion() {
		return textureRegion;
	}

	/**
	 * @param region new texture region
	 */
	public void setTextureRegion(TextureRegion region) {
		textureRegion = region;
	}

	/**
	 * Creates empty texture map object
	 */
	public TextureMapObject() {
		this(null);
	}

	/**
	 * Creates texture map object with the given region
	 * 
	 * @param textureRegion
	 */
	public TextureMapObject(TextureRegion textureRegion) {
		super();
		this.textureRegion = textureRegion;
	}

	/** @return The width of the texture in the units used by the map.
	 *    NA if the texture region is not set.
	 */
	public float getWidth() {
		TextureRegion region = getTextureRegion();  
		if(region!=null) {
			return region.getRegionWidth() * scaleX;	        // use the width of the texture region if there is one
		}
		return NA;
	}

	/** Sets the width of the texture in the units used by the map.
	 * This adjusts {@link #getScaleX scaleX} to scale the texture to the given width.
	 * @param value new width
	 * @throws IllegalStateException if {@link #getTextureRegion() textureRegion} is not set.
	 */
	public void setWidth(float value) {
		TextureRegion region = getTextureRegion();  
		if(region!=null) {
			scaleX = value / region.getRegionWidth();
		}
		else {
			throw new IllegalStateException("TextureMapObject: textureRegion must be assigned before width");
		}
	}

	/** @return The height of the texture in the units used by the map.
	 *    NA if the texture region is not set.
	 */
	public float getHeight() {
		TextureRegion region = getTextureRegion();  
		if(region!=null) {
			return region.getRegionHeight() * scaleY;         // use the height of the texture region if there is one
		}
		return NA;
	}

	/** Sets the height of the texture in the units used by the map.
	 * This adjusts {@link #getScaleY() scaleY} to scale the texture to the given height.
	 * @param value new height
	 * @throws IllegalStateException if {@link #getTextureRegion() textureRegion} is not set.
	 */
	public void setHeight(float value) {
		TextureRegion region = getTextureRegion();  
		if(region!=null) {
			scaleY = value / region.getRegionHeight();
		}
		else {
			throw new IllegalStateException("TextureMapObject: textureRegion must be assigned before width");
		}
	}

}
