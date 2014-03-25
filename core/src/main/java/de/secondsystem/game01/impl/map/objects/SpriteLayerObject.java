package de.secondsystem.game01.impl.map.objects;

import java.util.Map;

import org.jsfml.graphics.RenderTarget;
import org.jsfml.system.Vector2f;

import de.secondsystem.game01.impl.graphic.SpriteWrappper;
import de.secondsystem.game01.impl.map.IGameMap;
import de.secondsystem.game01.impl.map.IGameMap.WorldId;
import de.secondsystem.game01.impl.map.ILayerObject;
import de.secondsystem.game01.impl.map.Tileset;
import de.secondsystem.game01.model.Attributes;
import de.secondsystem.game01.model.Attributes.Attribute;

/**
 * TODO: tinting
 * @author lowkey
 *
 */
public class SpriteLayerObject implements ILayerObject {

	public static final LayerObjectType TYPE_UUID = LayerObjectType.getByType(SpriteLayerObject.class);
	
	private SpriteWrappper sprite;
	
	private int tileId;
	
	private int worldMask;
	
	public SpriteLayerObject(Tileset tileset, int worldId, int tileId, float x, float y, float rotation) {
		this(tileset, worldId, tileId, x, y, rotation, 0, 0);
	}
	public SpriteLayerObject(Tileset tileset, int worldId, int tileId, float x, float y, float rotation, float width, float height) {
		this.tileId = tileId;
		this.worldMask = worldId;
		sprite = new SpriteWrappper(tileset.get(tileId), tileset.getNormals(tileId), tileset.getClip(tileId));
		sprite.setPosition(new Vector2f(x, y));
		sprite.setRotation(rotation);
		sprite.setDimensions(width>0?width:sprite.getWidth(), height>0?height:sprite.getHeight());
	}
	
	public void setTile(Tileset tileset, int tileId) {
		this.tileId = tileId;
		sprite.setTexture(tileset.get(tileId), tileset.getNormals(tileId), tileset.getClip(tileId));
	}
	
	@Override
	public void draw(RenderTarget rt) {
		sprite.draw(rt);
	}
	
	@Override
	public void setDimensions(float width, float height) {
		sprite.setDimensions(width, height);
	}

	@Override
	public boolean inside(Vector2f point) {
		return sprite.inside(point);
	}

	@Override
	public void setPosition(Vector2f pos) {
		sprite.setPosition(pos);
	}

	@Override
	public void setRotation(float degree) {
		sprite.setRotation(degree);
	}

	@Override
	public float getHeight() {
		return sprite.getHeight();
	}

	@Override
	public float getWidth() {
		return sprite.getWidth();
	}

	@Override
	public float getRotation() {
		return sprite.getRotation();
	}

	@Override
	public Vector2f getPosition() {
		return sprite.getPosition();
	}

	@Override
	public boolean isInWorld(WorldId worldId) {
		return (worldMask & worldId.id)!=0;
	}

	@Override
	public void setWorld(WorldId worldId, boolean exists) {
		if( exists )
			worldMask|=worldId.id;
		else
			worldMask&=~worldId.id;
	}
	
	@Override
	public LayerObjectType typeUuid() {
		return TYPE_UUID;
	}

	@Override
	public Attributes serialize() {
		return new Attributes(
				new Attribute("$type", typeUuid().shortId),
				new Attribute("world", worldMask),
				new Attribute("tile", tileId),
				new Attribute("x", getPosition().x),
				new Attribute("y", getPosition().y),
				new Attribute("rotation", getRotation()),
				new Attribute("width", getWidth()),
				new Attribute("height", getHeight())
		);
	}
	
	public static SpriteLayerObject create(IGameMap map, Map<String, Object> attributes) {
		try {
			return new SpriteLayerObject(
					map.getTileset(),
					((Number)attributes.get("world")).intValue(),
					((Number)attributes.get("tile")).intValue(), 
					((Number)attributes.get("x")).floatValue(),
					((Number)attributes.get("y")).floatValue(),
					((Number)attributes.get("rotation")).floatValue(),
					((Number)attributes.get("width")).floatValue(),
					((Number)attributes.get("height")).floatValue());
		
		} catch( ClassCastException | NullPointerException e ) {
			throw new Error( "Invalid attributes: "+attributes, e );
		}
	}
}
