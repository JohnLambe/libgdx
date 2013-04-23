package com.badlogic.gdx.maps.tiled;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.SynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.ImageResolver;
import com.badlogic.gdx.maps.ImageResolver.AssetManagerImageResolver;
import com.badlogic.gdx.maps.ImageResolver.DirectImageResolver;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.objects.TextureMapObject;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Polyline;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;

/**
 * @brief synchronous loader for TMX maps created with the Tiled tool.
 * 
 * All layer encodings except XML are supported. (This is configured in the Map Properties dialog in Tiled).
 * 
 * Object and Tile Layers, but not Image Layers are currently loaded.
 * 
 * <h2>Global and Layer Custom Properties</h2>
 * <p>
 * The following properties are assigned in the TiledMap.getProperties() collection,
 * in addition to any custom properties set in the .TMX file:
 * <table border='1'>
 * <tr>
 * <th>Property name</th>
 * <th>Format</th>
 * <th>Description</th>
 * <th>Applicability</th>
 * </tr>
 * <tr>
 * <td>orientation</td>
 * <td>"orthogonal" or "isometric"</td>
 * <td>&nbsp;</td>
 * <td>Map</td>
 * </tr>
 * <tr>
 * <td>backgroundcolor</td>
 * <td>"#" followed by 6 hexadecimal digits (a 24-bit color value). Optional.</td>
 * <td>The configured background color of the map. This is optional in .TMX files</td>
 * <td>Map</td>
 * </tr>
 * <tr>
 * <td>width</td>
 * <td>integer. Always populated.</td>
 * <td>Width of the tilemap in cells.</td>
 * <td>Map and Layer</td>
 * </tr>
 * <tr>
 * <td>height</td>
 * <td>integer. Always populated.</td>
 * <td>Height of the tilemap in cells.</td>
 * <td>Map and Layer</td>
 * </tr>
 * <tr>
 * <td>tilewidth</td>
 * <td>integer (defaults to 0)</td>
 * <td>Width of each tile in pixels.</td>
 * <td>Map and Layer*</td>
 * </tr>
 * <tr>
 * <td>tileheight</td>
 * <td>integer (defaults to 0)</td>
 * <td>Height of each tile in pixels.</td>
 * <td>Map and Layer*</td>
 * </tr>
 * <tr>
 * <td colspan='4'>*: These are not configurable at layer level in Tiled,
 * and currently always set to 0 on loading.</td>
 * </tr>
 * </table>
 * If custom properties defined in the .TMX file conflict with these, they override these.
 * </p>
 * 
 * <h2>Map Objects</h2>
 * <p>
 * For map objects (objects placed on an Object Layer in Tiled),
 * the following properties are assigned in the MapObject.getProperties() collection,
 * in addition to any custom properties set in the .TMX file:
 * <table border='1'>
 * <tr>
 * <th>Property name</th>
 * <th>Format</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>name</td>
 * <td>string. Always populated; may be "".</td>
 * <td>the object name editable in Tiled</td>
 * </tr>
 * <tr>
 * <td>type</td>
 * <td>string. Always populated; may be "".</td>
 * <td>the object type editable in Tiled</td>
 * </tr>
 * <tr>
 * <td>x</td>
 * <td>integer</td>
 * <td>X coordinate in pixels (0 is the left of the map; the positive X direction is right)</td>
 * </tr>
 * <tr>
 * <td>y</td>
 * <td>integer</td>
 * <td>Y coordinate in pixels (0 is the top of the map and the positive Y direction is down,
 * regardless of the TmxMapLoader.Parameters.yUp parameter).</td>
 * </tr>
 * </table>
 * </p>
 * 
 * <h3>'Tile' Map Objects</h3>
 * <p>
 * For Tile map objects (i.e. objects placed with the 'Insert Tile' tool in Tiled),
 * the following custom properties, if defined in the .TMX file, cause the property of TextureMapObject with
 * the same name to be assigned:
 * <ul>
 * <li><em>rotation</em> (in radians; defaults to 0)</li>
 * <li><em>rotationDeg</em> (in degrees; defaults to 0; ignored if 'rotation' is also present; Note: This is a shortened version of the property name)</li>
 * <li><em>scaleX</em> (defaults to 1.0 - no scaling)</li>
 * <li><em>scaleY</em> (defaults to 1.0 - no scaling)</li>
 * <li><em>width</em> (a default of the tile width is used)</li>
 * <li><em>height</em> (a default of the tile height is used)</li>
 * </ul>
 * When used, these cause the object to be transformed when rendered by a TiledMapRenderer with support for these,
 * but do not affect its display in Tiled, and are not standard usage for Tiled.
 * </p>
 * <p>
 * The following property is added to the MapObject.getProperties() collection:
 * <table border='1'>
 * <tr>
 * <th>Property name</th>
 * <th>Format</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>gid</td>
 * <td>integer. Always populated for Tile map objects.</td>
 * <td>The 'gid' (global tile ID) of the Tile map object.</td>
 * </tr>
 * </table>
 * If a custom property defined in the .TMX file conflicts with this, it overrides this.
 * </p>
 * <p>
 * The {@link com.badlogic.gdx.maps.objects.TextureMapObject#setOrigin(float,float) origin} 
 * of the object is set to the center.
 * </p>
 */
public class TmxMapLoader extends AsynchronousAssetLoader<TiledMap, TmxMapLoader.Parameters> {

	public static class Parameters extends AssetLoaderParameters<TiledMap> {
		/** Whether to load the map for a y-up coordinate system */
		public boolean yUp             = true;
		/** generate mipmaps? **/
		public boolean generateMipMaps = false;
		/** The TextureFilter to use for minification **/
		public TextureFilter textureMinFilter = TextureFilter.Nearest;
		/** The TextureFilter to use for magnification **/
		public TextureFilter textureMagFilter = TextureFilter.Nearest;
	}

    // Flags as bits in 'gid' value of cells:
	protected static final int FLAG_FLIP_HORIZONTALLY = 0x80000000;
	protected static final int FLAG_FLIP_VERTICALLY   = 0x40000000;
	protected static final int FLAG_FLIP_DIAGONALLY   = 0x20000000;
	protected static final int MASK_CLEAR             = 0xE0000000;

    // Special properties for Tile map objects (key values for custom properties editable in Tiled):
	// The name of the constant after the underscore, is the name of the
	// corresponding property on TextureMapObject, except for the capitalization.
	protected static final String PROPERTY_ROTATION = "rotation";
	protected static final String PROPERTY_ROTATIONDEGREES = "rotationDeg";
	protected static final String PROPERTY_SCALEX = "scaleX";
	protected static final String PROPERTY_SCALEY = "scaleY";
	protected static final String PROPERTY_WIDTH  = "width";
	protected static final String PROPERTY_HEIGHT = "height";

	protected XmlReader        xml                    = new XmlReader();
	/** The root node of the .TMX (XML) file.     */
	protected Element          root;
	protected boolean          yUp;

	/** The total width of the map in pixels. Assigned in loadTileMap
	 * and can be used in loadTileLayer, loadObjectGroup and loadObject.
	 */
	protected int              mapWidthInPixels;
	protected int              mapHeightInPixels;

	/** The map loaded. During loading this may not be assigned, or may be the
	 * previous map loaded.
	 */
	protected TiledMap         map;

	public TmxMapLoader () {
		super(new InternalFileHandleResolver());
	}

	/** Creates loader
	 * 
	 * @param resolver */
	public TmxMapLoader (FileHandleResolver resolver) {
		super(resolver);
	}

	/**
	 * Loads the {@link TiledMap} from the given file. The file is
	 * resolved via the {@link FileHandleResolver} set in the constructor
	 * of this class. By default it will resolve to an internal file. The
	 * map will be loaded for a y-up coordinate system.
	 * 
	 * @param fileName the filename
	 * @return the TiledMap
	 */
	public TiledMap load (String fileName) {
		return load(fileName, new TmxMapLoader.Parameters());
	}

	/**
	 * Loads the {@link TiledMap} from the given file. The file is
	 * resolved via the {@link FileHandleResolver} set in the constructor
	 * of this class. By default it will resolve to an internal file.
	 * 
	 * @param fileName the filename
	 * @param parameters specifies whether to use y-up, generate mip maps etc.
	 * @return the TiledMap
	 */
	public TiledMap load (String fileName, TmxMapLoader.Parameters parameters) {
		try {
			this.yUp = parameters.yUp;
			FileHandle tmxFile = resolve(fileName);
			root = xml.parse(tmxFile);               // open the file
			ObjectMap<String, Texture> textures = new ObjectMap<String, Texture>();
			for (FileHandle textureFile : loadTilesets(root, tmxFile)) {
				Texture texture = new Texture(textureFile, parameters.generateMipMaps);
				texture.setFilter(parameters.textureMinFilter, parameters.textureMagFilter);				
				textures.put(textureFile.path(), texture);
			}
			DirectImageResolver imageResolver = new DirectImageResolver(textures);
			TiledMap map = loadTilemap(root, tmxFile, imageResolver);
			map.setOwnedTextures(textures.values().toArray());
			return map;
		} catch (IOException e) {
			throw new GdxRuntimeException("Couldn't load tilemap '" + fileName + "'", e);
		}
	}

	@Override
	public void loadAsync (AssetManager manager, String fileName, Parameters parameter) {
		map = null;

		FileHandle tmxFile = resolve(fileName);
		if (parameter != null) {
			yUp = parameter.yUp;
		} else {
			yUp = true;
		}
		try {
			map = loadTilemap(root, tmxFile, new AssetManagerImageResolver(manager));
		} catch (Exception e) {
			throw new GdxRuntimeException("Couldn't load tilemap '" + fileName + "'", e);
		}
	}

	@Override
	public TiledMap loadSync (AssetManager manager, String fileName, Parameters parameter) {
		return map;
	}

	/**
	 * Retrieves TiledMap resource dependencies
	 * 
	 * @param fileName
	 * @param parameter not used for now
	 * @return dependencies for the given .tmx file
	 */
	@Override
	public Array<AssetDescriptor> getDependencies (String fileName, Parameters parameter) {
		Array<AssetDescriptor> dependencies = new Array<AssetDescriptor>();
		try {
			FileHandle tmxFile = resolve(fileName);
			root = xml.parse(tmxFile);
			boolean generateMipMaps = (parameter != null ? parameter.generateMipMaps : false);
			TextureLoader.TextureParameter texParams = new TextureParameter();
			texParams.genMipMaps = generateMipMaps;
			if (parameter != null) {
				texParams.minFilter = parameter.textureMinFilter;
				texParams.magFilter = parameter.textureMagFilter;
			}
			for (FileHandle image : loadTilesets(root, tmxFile)) {
				dependencies.add(new AssetDescriptor(image.path(), Texture.class, texParams));
			}
			return dependencies;
		} catch (IOException e) {
			throw new GdxRuntimeException("Couldn't load tilemap '" + fileName + "'", e);
		}
	}

	/**
	 * Loads the map data, given the XML root element and an {@link ImageResolver} used
	 * to return the tileset Textures
	 * 
	 * @param root the XML root element
	 * @param tmxFile the Filehandle of the tmx file
	 * @param imageResolver the {@link ImageResolver}
	 * @return the {@link TiledMap}
	 */
	protected TiledMap loadTilemap (Element root, FileHandle tmxFile, ImageResolver imageResolver) {
		TiledMap map = new TiledMap();

		String mapOrientation = root.getAttribute("orientation", null);
		int mapWidth = root.getIntAttribute("width", 0);
		int mapHeight = root.getIntAttribute("height", 0);
		int tileWidth = root.getIntAttribute("tilewidth", 0);
		int tileHeight = root.getIntAttribute("tileheight", 0);
		String mapBackgroundColor = root.getAttribute("backgroundcolor", null);
		    // if present, this holds a 24-bit color in the format: "#" followed by 6 hexadecimal digits

		MapProperties mapProperties = map.getProperties();
		if (mapOrientation != null) {
			mapProperties.put("orientation", mapOrientation);
		}
		mapProperties.put("width", mapWidth);
		mapProperties.put("height", mapHeight);
		mapProperties.put("tilewidth", tileWidth);
		mapProperties.put("tileheight", tileHeight);
		if (mapBackgroundColor != null) {
			mapProperties.put("backgroundcolor", mapBackgroundColor);
		}
		mapWidthInPixels = mapWidth * tileWidth;
		mapHeightInPixels = mapHeight * tileHeight;

		Element properties = root.getChildByName("properties");   // Map properties (not specific to a layer)
		if (properties != null) {
			loadProperties(map.getProperties(), properties);      // this override any special properties assigned above
		}
		Array<Element> tilesets = root.getChildrenByName("tileset");   // all tilesets
		for (Element element : tilesets) {
			loadTileSet(map, element, tmxFile, imageResolver);
			root.removeChild(element);
		}
		for (int i = 0, j = root.getChildCount(); i < j; i++) {   // iterate through all child nodes
			Element element = root.getChild(i);
			String name = element.getName();
			if (name.equals("layer")) {             // a tiled map layer
				loadTileLayer(map, element);
			}
			else if (name.equals("objectgroup")) {  // an object layer/group
				loadObjectGroup(map, element);
			}
			else if (name.equals("imagelayer")) {   // an image layer
				loadImageLayer(map, element);
			}
		}
		return map;
	}

	/**
	 * Load an image layer (a layer containing a single image).
	 * 
	 * This is currently not implemented here but could be implemented in a subclass.
	 * If implementing in a subclass, do not call the inherited implementation,
	 * since this may be implemented in a later version.
	 * 
	 * @param map the map to load the layer into.
	 * @param element the 'imagelayer' XML tag.
	 */
	protected void loadImageLayer (TiledMap map, Element element) {
	}

	/**
	 * Loads the tilesets
	 * 
	 * @param root the root XML element
	 * @return a list of filenames for images containing tiles
	 * @throws IOException
	 */
	protected Array<FileHandle> loadTilesets (Element root, FileHandle tmxFile) throws IOException {
		Array<FileHandle> images = new Array<FileHandle>();
		for (Element tileset : root.getChildrenByName("tileset")) {
			String source = tileset.getAttribute("source", null);
			FileHandle image = null;
			if (source != null) {
				FileHandle tsx = getRelativeFileHandle(tmxFile, source);
				tileset = xml.parse(tsx);
				String imageSource = tileset.getChildByName("image").getAttribute("source");
				image = getRelativeFileHandle(tsx, imageSource);
			} else {
				String imageSource = tileset.getChildByName("image").getAttribute("source");
				image = getRelativeFileHandle(tmxFile, imageSource);
			}
			images.add(image);
		}
		return images;
	}

	/**
	 * Loads the specified tileset data, adding it to the collection of the specified map, given the XML element, the tmxFile
	 * and an {@link ImageResolver} used to retrieve the tileset Textures.
	 * 
	 * <p>
	 * Default tileset's property keys that are loaded by default are:
	 * </p>
	 * 
	 * <ul>
	 * <li><em>firstgid</em>, (int, defaults to 1) the first valid global id used for tile numbering</li>
	 * <li><em>imagesource</em>, (String, defaults to empty string) the tileset source image filename</li>
	 * <li><em>imagewidth</em>, (int, defaults to 0) the tileset source image width</li>
	 * <li><em>imageheight</em>, (int, defaults to 0) the tileset source image height</li>
	 * <li><em>tilewidth</em>, (int, defaults to 0) the tile width</li>
	 * <li><em>tileheight</em>, (int, defaults to 0) the tile height</li>
	 * <li><em>margin</em>, (int, defaults to 0) the tileset margin</li>
	 * <li><em>spacing</em>, (int, defaults to 0) the tileset spacing</li>
	 * </ul>
	 * 
	 * <p>
	 * The values are extracted from the specified Tmx file, if a value can't be found then the default is used.
	 * </p>
	 * 
	 * @param map the Map whose tilesets collection will be populated
	 * @param element the XML element identifying the tileset to load
	 * @param tmxFile the Filehandle of the tmx file
	 * @param imageResolver the {@link ImageResolver}
	 */
	protected void loadTileSet(TiledMap map, Element element, FileHandle tmxFile,
		                       ImageResolver imageResolver) {
		if (element.getName().equals("tileset")) {
			String name = element.get("name", null);
			int firstgid = element.getIntAttribute("firstgid", 1);
			int tilewidth = element.getIntAttribute("tilewidth", 0);
			int tileheight = element.getIntAttribute("tileheight", 0);
			int spacing = element.getIntAttribute("spacing", 0);
			int margin = element.getIntAttribute("margin", 0);
			String source = element.getAttribute("source", null);

			String imageSource = "";
			int imageWidth = 0, imageHeight = 0;

			FileHandle image = null;
			if (source != null) {
				FileHandle tsx = getRelativeFileHandle(tmxFile, source);
				try {
					element = xml.parse(tsx);
					name = element.get("name", null);
					tilewidth = element.getIntAttribute("tilewidth", 0);
					tileheight = element.getIntAttribute("tileheight", 0);
					spacing = element.getIntAttribute("spacing", 0);
					margin = element.getIntAttribute("margin", 0);
					imageSource = element.getChildByName("image").getAttribute("source");
					imageWidth = element.getChildByName("image").getIntAttribute("width", 0);
					imageHeight = element.getChildByName("image").getIntAttribute("height", 0);
					image = getRelativeFileHandle(tsx, imageSource);
				} catch (IOException e) {
					throw new GdxRuntimeException("Error parsing external tileset", e);
				}
			} else {
				imageSource = element.getChildByName("image").getAttribute("source");
				imageWidth = element.getChildByName("image").getIntAttribute("width", 0);
				imageHeight = element.getChildByName("image").getIntAttribute("height", 0);
				image = getRelativeFileHandle(tmxFile, imageSource);
			}

			TextureRegion texture = imageResolver.getImage(image.path());

			TiledMapTileSet tileset = new TiledMapTileSet();
			MapProperties props = tileset.getProperties();
			tileset.setName(name);
			props.put("firstgid", firstgid);
			props.put("imagesource", imageSource);
			props.put("imagewidth", imageWidth);
			props.put("imageheight", imageHeight);
			props.put("tilewidth", tilewidth);
			props.put("tileheight", tileheight);
			props.put("margin", margin);
			props.put("spacing", spacing);

			int stopWidth = texture.getRegionWidth() - tilewidth;
			int stopHeight = texture.getRegionHeight() - tileheight;

			int id = firstgid;

			for (int y = margin; y <= stopHeight; y += tileheight + spacing) {
				for (int x = margin; x <= stopWidth; x += tilewidth + spacing) {
					TextureRegion tileRegion = new TextureRegion(texture, x, y,
					                                             tilewidth, tileheight);
					if (!yUp) {
						tileRegion.flip(false, true);
					}
					TiledMapTile tile = new StaticTiledMapTile(tileRegion);
					tile.setId(id);
					tileset.putTile(id++, tile);
				}
			}

			Array<Element> tileElements = element.getChildrenByName("tile");

			for (Element tileElement : tileElements) {
				int localtid = tileElement.getIntAttribute("id", 0);
				TiledMapTile tile = tileset.getTile(firstgid + localtid);
				if (tile != null) {
					Element properties = tileElement.getChildByName("properties");
					if (properties != null) {
						loadProperties(tile.getProperties(), properties);
					}
				}
			}

			Element properties = element.getChildByName("properties");
			if (properties != null) {
				loadProperties(tileset.getProperties(), properties);
			}
			map.getTileSets().addTileSet(tileset);
		}
	}

	/**
	 * Load one tile map layer (a 'layer' tag).
	 * 
	 * @param map  the map into which the layer is to be loaded.
	 * @param element the XML layer tag.
	 */
	protected void loadTileLayer (TiledMap map, Element element) {
		if (element.getName().equals("layer")) {
			String name = element.getAttribute("name", null);
			int width = element.getIntAttribute("width", 0);
			int height = element.getIntAttribute("height", 0);
			int tileWidth = element.getParent().getIntAttribute("tilewidth", 0);
			int tileHeight = element.getParent().getIntAttribute("tileheight", 0);
			boolean visible = element.getIntAttribute("visible", 1) == 1;
			float opacity = element.getFloatAttribute("opacity", 1.0f);
			TiledMapTileLayer layer = new TiledMapTileLayer(width, height, tileWidth, tileHeight);
			layer.setVisible(visible);
			layer.setOpacity(opacity);
			layer.setName(name);

			TiledMapTileSets tilesets = map.getTileSets();

			Element data = element.getChildByName("data");
			String encoding = data.getAttribute("encoding", null);
			String compression = data.getAttribute("compression", null);
			if (encoding == null) {
				// no 'encoding' attribute means that the encoding is XML
				throw new GdxRuntimeException("Unsupported encoding (XML) for TMX Layer Data");
			}
			if (encoding.equals("csv")) {
				String[] array = data.getText().split(",");
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int id = (int)Long.parseLong(array[y * width + x].trim());

						final boolean flipHorizontally = ((id & FLAG_FLIP_HORIZONTALLY) != 0);
						final boolean flipVertically = ((id & FLAG_FLIP_VERTICALLY) != 0);
						final boolean flipDiagonally = ((id & FLAG_FLIP_DIAGONALLY) != 0);

						id = id & ~MASK_CLEAR;

						tilesets.getTile(id);
						TiledMapTile tile = tilesets.getTile(id);
						if (tile != null) {
							Cell cell = new Cell();
							if (flipDiagonally) {
								if (flipHorizontally && flipVertically) {
									cell.setFlipHorizontally(true);
									cell.setRotation(Cell.ROTATE_270);
								} else if (flipHorizontally) {
									cell.setRotation(Cell.ROTATE_270);
								} else if (flipVertically) {
									cell.setRotation(Cell.ROTATE_90);
								} else {
									cell.setFlipVertically(true);
									cell.setRotation(Cell.ROTATE_270);
								}
							} else {
								cell.setFlipHorizontally(flipHorizontally);
								cell.setFlipVertically(flipVertically);
							}
							cell.setTile(tile);
							layer.setCell(x, yUp ? height - 1 - y : y, cell);
						}
					}
				}
			} else {
				if (encoding.equals("base64")) {
					byte[] bytes = Base64Coder.decode(data.getText());
					if (compression == null) {
						int read = 0;
						for (int y = 0; y < height; y++) {
							for (int x = 0; x < width; x++) {

								int id = unsignedByteToInt(bytes[read++]) | unsignedByteToInt(bytes[read++]) << 8
									| unsignedByteToInt(bytes[read++]) << 16 | unsignedByteToInt(bytes[read++]) << 24;

								final boolean flipHorizontally = ((id & FLAG_FLIP_HORIZONTALLY) != 0);
								final boolean flipVertically = ((id & FLAG_FLIP_VERTICALLY) != 0);
								final boolean flipDiagonally = ((id & FLAG_FLIP_DIAGONALLY) != 0);

								id = id & ~MASK_CLEAR;

								tilesets.getTile(id);
								TiledMapTile tile = tilesets.getTile(id);
								if (tile != null) {
									Cell cell = new Cell();
									if (flipDiagonally) {
										if (flipHorizontally && flipVertically) {
											cell.setFlipHorizontally(true);
											cell.setRotation(Cell.ROTATE_270);
										} else if (flipHorizontally) {
											cell.setRotation(Cell.ROTATE_270);
										} else if (flipVertically) {
											cell.setRotation(Cell.ROTATE_90);
										} else {
											cell.setFlipVertically(true);
											cell.setRotation(Cell.ROTATE_270);
										}
									} else {
										cell.setFlipHorizontally(flipHorizontally);
										cell.setFlipVertically(flipVertically);
									}
									cell.setTile(tile);
									layer.setCell(x, yUp ? height - 1 - y : y, cell);
								}
							}
						}
					} else if (compression.equals("gzip")) {
						GZIPInputStream GZIS = null;
						try {
							GZIS = new GZIPInputStream(new ByteArrayInputStream(bytes), bytes.length);
						} catch (IOException e) {
							throw new GdxRuntimeException("Error Reading TMX Layer Data - IOException: " + e.getMessage(), e);
						}

						byte[] temp = new byte[4];
						for (int y = 0; y < height; y++) {
							for (int x = 0; x < width; x++) {
								try {
									GZIS.read(temp, 0, 4);
									int id = unsignedByteToInt(temp[0]) | unsignedByteToInt(temp[1]) << 8
										| unsignedByteToInt(temp[2]) << 16 | unsignedByteToInt(temp[3]) << 24;

									final boolean flipHorizontally = ((id & FLAG_FLIP_HORIZONTALLY) != 0);
									final boolean flipVertically = ((id & FLAG_FLIP_VERTICALLY) != 0);
									final boolean flipDiagonally = ((id & FLAG_FLIP_DIAGONALLY) != 0);

									id = id & ~MASK_CLEAR;

									tilesets.getTile(id);
									TiledMapTile tile = tilesets.getTile(id);
									if (tile != null) {
										Cell cell = new Cell();
										if (flipDiagonally) {
											if (flipHorizontally && flipVertically) {
												cell.setFlipHorizontally(true);
												cell.setRotation(Cell.ROTATE_270);
											} else if (flipHorizontally) {
												cell.setRotation(Cell.ROTATE_270);
											} else if (flipVertically) {
												cell.setRotation(Cell.ROTATE_90);
											} else {
												cell.setFlipVertically(true);
												cell.setRotation(Cell.ROTATE_270);
											}
										} else {
											cell.setFlipHorizontally(flipHorizontally);
											cell.setFlipVertically(flipVertically);
										}
										cell.setTile(tile);
										layer.setCell(x, yUp ? height - 1 - y : y, cell);
									}
								} catch (IOException e) {
									throw new GdxRuntimeException("Error Reading TMX Layer Data", e);
								}
							}
						}
					} else if (compression.equals("zlib")) {
						Inflater zlib = new Inflater();

						byte[] temp = new byte[4];

						zlib.setInput(bytes, 0, bytes.length);

						for (int y = 0; y < height; y++) {
							for (int x = 0; x < width; x++) {
								try {
									zlib.inflate(temp, 0, 4);
									int id =
										unsignedByteToInt(temp[0]) |
										unsignedByteToInt(temp[1]) << 8 |
										unsignedByteToInt(temp[2]) << 16 |
										unsignedByteToInt(temp[3]) << 24;

									final boolean flipHorizontally = ((id & FLAG_FLIP_HORIZONTALLY) != 0);
									final boolean flipVertically = ((id & FLAG_FLIP_VERTICALLY) != 0);
									final boolean flipDiagonally = ((id & FLAG_FLIP_DIAGONALLY) != 0);

									id = id & ~MASK_CLEAR;

									tilesets.getTile(id);
									TiledMapTile tile = tilesets.getTile(id);
									if (tile != null) {
										Cell cell = new Cell();
										if (flipDiagonally) {
											if (flipHorizontally && flipVertically) {
												cell.setFlipHorizontally(true);
												cell.setRotation(Cell.ROTATE_270);
											} else if (flipHorizontally) {
												cell.setRotation(Cell.ROTATE_270);
											} else if (flipVertically) {
												cell.setRotation(Cell.ROTATE_90);
											} else {
												cell.setFlipVertically(true);
												cell.setRotation(Cell.ROTATE_270);
											}
										} else {
											cell.setFlipHorizontally(flipHorizontally);
											cell.setFlipVertically(flipVertically);
										}
										cell.setTile(tile);
										layer.setCell(x, yUp ? height - 1 - y : y, cell);
									}
								} catch (DataFormatException e) {
									throw new GdxRuntimeException("Error Reading TMX Layer Data",
										e);
								}
							}
						}
					}
				} else {   // any other value of 'encoding' is one we're not aware of, probably a feature of a future version of Tiled or
						   // another editor
					throw new GdxRuntimeException("Unrecognised encoding (" + encoding
					                              + ") for TMX Layer Data");
				}
			}
			Element properties = element.getChildByName("properties");
			if (properties != null) {
				loadProperties(layer.getProperties(), properties);
			}
			map.getLayers().add(layer);
		}
	}

	/**
	 * Loads an object group (object layer).
	 * @param map the map that this group is in.
	 * @param element the 'objectgroup' XML element for the object group.
	 */
	protected void loadObjectGroup (TiledMap map, Element element) {
		if (element.getName().equals("objectgroup")) {
			String name = element.getAttribute("name", null);
			MapLayer layer = new MapLayer();
			layer.setName(name);
			Element properties = element.getChildByName("properties");
			if (properties != null) {
				loadProperties(layer.getProperties(), properties);
			}

			for (Element objectElement : element.getChildrenByName("object")) {
				loadObject(map, layer, objectElement);
			}

			map.getLayers().add(layer);
		}
	}

	/**
	 * Loads a map object into a layer.
	 * @param layer The layer of the map which the object is on.
	 * @param element the 'object' XML element defining the object.
	 */
	protected void loadObject (TiledMap map, MapLayer layer, Element element) {
		if (element.getName().equals("object")) {
			MapObject object = null;
			int gid = -1;  // global tile ID ('gid' attribute of some elements).
			// This uniquely identifies a tile in any tileset of the map.

			int x = element.getIntAttribute("x", 0);
			int y = (yUp ? mapHeightInPixels - element.getIntAttribute("y", 0) : element
				.getIntAttribute("y", 0));

			// Get the width and height. Note: Tile objects do not specify these explicitly.
			int width = element.getIntAttribute("width", 0);
			int height = element.getIntAttribute("height", 0);

			if (element.getChildCount() > 0) {
				Element child = null;
				if ((child = element.getChildByName("polygon")) != null) {
					String[] points = child.getAttribute("points").split(" ");
					float[] vertices = new float[points.length * 2];
					for (int i = 0; i < points.length; i++) {
						String[] point = points[i].split(",");
						vertices[i * 2] = Integer.parseInt(point[0]);
						vertices[i * 2 + 1] = Integer.parseInt(point[1]);
						if (yUp) {
							vertices[i * 2 + 1] *= -1;
						}
					}
					Polygon polygon = new Polygon(vertices);
					polygon.setPosition(x, y);
					object = new PolygonMapObject(polygon);
				}
				else if ((child = element.getChildByName("polyline")) != null) {
					String[] points = child.getAttribute("points").split(" ");
					float[] vertices = new float[points.length * 2];
					for (int i = 0; i < points.length; i++) {
						String[] point = points[i].split(",");
						vertices[i * 2] = Integer.parseInt(point[0]);
						vertices[i * 2 + 1] = Integer.parseInt(point[1]);
						if (yUp) {
							vertices[i * 2 + 1] *= -1;
						}
					}
					Polyline polyline = new Polyline(vertices);
					polyline.setPosition(x, y);
					object = new PolylineMapObject(polyline);
				}
				else if ((child = element.getChildByName("ellipse")) != null) {
					object = new EllipseMapObject(x, yUp ? y - height : y, width, height);
				}
			}
			gid = element.getIntAttribute("gid", -1);
			if (object == null && gid > -1) {  // if not explicitly a shape and it has a 'gid'
				// this is a Tile object
				object = loadTileMapObject(map, layer, x, y, gid, element);
			}
			if (object == null) {   // if not any other type of object
				// it is a rectangle shape
				object = new RectangleMapObject(x, yUp ? y - height : y, width, height);
			}
			object.setName(element.getAttribute("name", null));
			String type = element.getAttribute("type", null);
			if (type != null) {
				object.getProperties().put("type", type);
			}
			object.getProperties().put("x", x);
			object.getProperties().put("y", yUp ? y - height : y);
			object.setVisible(element.getIntAttribute("visible", 1) == 1);
			Element properties = element.getChildByName("properties");
			if (properties != null) {  // if there is a 'properties' element (custom properties)
				loadProperties(object.getProperties(), properties);    // load them into the map object
			}
			
			object = loadedMapObject(map,layer,object);
			if(object != null) {                     // if not rejected by loadedMapObject
				layer.getObjects().add(object);
			}
		}
	}

	/**
	 * Loads a Tile map object (as placed with the 'Insert Tile' tool in Tiled).
	 * These may be used as game actors or movable objects.
	 * This can be overridden to provide custom behavior on loading these,
	 * including potentially returning a subclass of TextureMapObject.
	 * 
	 * @param map the map that contains this object.
	 * @param layer the map layer that contains this object.
	 * @param x the X-coordinate of the corner
	 * @param y the Y-coordinate of the corner
	 * @param gid the global tile id ('gid' attribute in the .TMX file) for this object
	 * @param element the 'object' XML tag representing this map object in the .TMX file.
	 * @return an object representing the map object
	 */
	protected TextureMapObject loadTileMapObject (TiledMap map, MapLayer layer,
		                                          int x, int y, int gid,
		                                          Element element) {
		TiledMapTile tile = map.getTileSets().getTile(gid);  // get the tile		
		// create the object, with the texture region of the tile:		
		TextureMapObject textureMapObject = new TextureMapObject(tile==null ? null : tile.getTextureRegion());
		textureMapObject.getProperties().put("gid", gid);   // populate the 'gid' so that game logic can use it
		textureMapObject.setCoordinates(x,y);               // set the coordinates of the corner

		// set Opacity to layer's opacity:
		// textureMapObject.setOpacity(layer.getOpacity());  //TODO should we do this? Or should the opacity be combined when rendering?
		return textureMapObject;		
	}
	
	/** 
	 * Called after fully loading a map object, and before adding it to the layer.
	 * This can be overridden to provide custom behavior.
	 * 
	 * @param map the map that contains this object.
	 * @param layer the map layer that contains this object.
	 * @param object the new map object.
	 * @return the given map object or an object that replaces it.
	 *     If null is returned, the map object is not added to the layer,
	 *     and will be freed if the implementation of this does not keep a reference to it.
	 * @see #loadObject
	 */
	protected MapObject loadedMapObject (TiledMap map, MapLayer layer, MapObject object) {
		if (object instanceof TextureMapObject) {
			object = loadedTextureMapObject(map, layer, (TextureMapObject)object);
		}
		return object;
	}

	/**
	 * Called by {@link #loadedMapObject(TiledMap, MapLayer, MapObject)} when the object is a
	 * TextureMapObject.
	 * 
	 * @param map the map that contains this object.
	 * @param layer the map layer that contains this object.
	 * @param object the new map object.
	 * @return the given map object or an object that replaces it.
	 *     If null is returned, the map object is not added to the layer,
	 *     and will be freed if the implementation of this does not keep a reference to it.
	 */
	protected MapObject loadedTextureMapObject (TiledMap map, MapLayer layer, TextureMapObject object) {
		MapProperties properties = object.getProperties();
		if (properties != null) {   // special properties for Tile map objects:
			// custom properties to transform the object
			// (even though the level designer can't see the transformation in Tiled):
			if(properties.containsKey(PROPERTY_ROTATION)) {                         // in radians
				object.setRotation(properties.getFloat(PROPERTY_ROTATION, 0.0f));
			} else if(properties.containsKey(PROPERTY_ROTATIONDEGREES)) {               // in degrees
				object.setRotationDegrees(properties.getFloat(PROPERTY_ROTATIONDEGREES, 0.0f));
			}
			object.setScaleX(properties.getFloat(PROPERTY_SCALEX, 1.0f));
			object.setScaleY(properties.getFloat(PROPERTY_SCALEY, 1.0f));
			float value = properties.getFloat(PROPERTY_WIDTH, -1.0f);
			if (value > -1) { // if 'width' is defined as a custom property
				object.setWidth(value);
			}
			value = object.getProperties().getFloat(PROPERTY_HEIGHT, -1.0f);
			if (value > -1) {
				object.setHeight(value);
			}
		}

		// set the origin to the center (more useful for rotating and scaling):
		// (This is done last in case the width or height is changed above)
		object.setOrigin(object.getWidth()/2f, object.getHeight()/2f);
		
		return object;
	}

	/** Load a 'properties' element into a MapProperties.
	 * The children of this element provide a set of name/value pairs.
	 * @param properties the collection to be populated from the XML.
	 * @param element the element to load. */
	protected void loadProperties (MapProperties properties, Element element) {
		if (element.getName().equals("properties")) {
			for (Element property : element.getChildrenByName("property")) {
				String name = property.getAttribute("name", null);
				String value = property.getAttribute("value", null);
				if (value == null) {
					value = property.getText();
				}
				properties.put(name, value);
			}
		}
	}

	protected static FileHandle getRelativeFileHandle (FileHandle file, String path) {
		StringTokenizer tokenizer = new StringTokenizer(path, "\\/");
		FileHandle result = file.parent();
		while (tokenizer.hasMoreElements()) {
			String token = tokenizer.nextToken();
			if (token.equals("..")) {
				result = result.parent();
			} else {
				result = result.child(token);
			}
		}
		return result;
	}

	protected static int unsignedByteToInt (byte b) {
		return (int)b & 0xFF;
	}

}
