package de.secondsystem.game01.impl.map.objects;

import org.jsfml.graphics.RenderTarget;
import org.jsfml.system.Vector2f;

import de.secondsystem.game01.impl.game.entities.IGameEntity;
import de.secondsystem.game01.impl.game.entities.IGameEntityManager;
import de.secondsystem.game01.impl.map.IGameMap.WorldId;
import de.secondsystem.game01.impl.map.ILayer;
import de.secondsystem.game01.impl.map.ILayerObject;
import de.secondsystem.game01.impl.map.LayerType;
import de.secondsystem.game01.model.Attributes;
import de.secondsystem.game01.model.GameException;

public class EntityLayer implements ILayer {

	private final IGameEntityManager manager;
	
	private final WorldId worldId;
	
	private boolean show;
	
	public EntityLayer(LayerType type, IGameEntityManager entityManager, WorldId worldId) {
		this.manager = entityManager;
		this.worldId = worldId;
		this.show = type.visible;
	}

	@Override
	public void draw(RenderTarget rt) {
		manager.draw(worldId, rt);
	}

	@Override
	public void addNode(ILayerObject obj) {
		if( obj instanceof EntityLayerObject && ((EntityLayerObject)obj).entity==null ) {
			manager.create(((EntityLayerObject)obj).uuid, ((EntityLayerObject)obj).type, ((EntityLayerObject)obj).attributes);
		} else
			throw new GameException("Unable to add something other than an entity to the EntityLayer.");
	}

	@Override
	public ILayerObject findNode(Vector2f point) {
		final IGameEntity entity = manager.findEntity(point);
		return entity!=null ? new EntityLayerObject(entity) : null;
	}

	@Override
	public void remove(ILayerObject obj) {
		if( obj instanceof EntityLayerObject && ((EntityLayerObject)obj).uuid!=null ) {
			manager.destroy(((EntityLayerObject)obj).uuid);
		} else
			throw new GameException("Unable to remove something other than an entity from the EntityLayer.");
	}

	@Override
	public void update(long frameTimeMs) {
	}

	@Override
	public boolean isVisible() {
		return show;
	}

	@Override
	public boolean setVisible(boolean visible) {
		return show = visible;
	}

	@Override
	public Attributes serialize() {
		return null; // noop
	}

}