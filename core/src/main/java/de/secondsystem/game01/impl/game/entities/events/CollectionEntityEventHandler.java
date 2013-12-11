package de.secondsystem.game01.impl.game.entities.events;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import de.secondsystem.game01.impl.game.entities.IGameEntity;
import de.secondsystem.game01.impl.map.IGameMap;

public class CollectionEntityEventHandler implements IEntityEventHandler {

	private final ListMultimap<EntityEventType, IEntityEventHandler> handlers = ArrayListMultimap.create();
	
	private UUID uuid;
	
	public CollectionEntityEventHandler(UUID uuid) {
		this.uuid = uuid;
	}
	
	public CollectionEntityEventHandler() {
	}
	
	public void addEntityEventHandler(EntityEventType type, IEntityEventHandler handler) {
		handlers.put(type, handler);
	}
	
	@Override
	public Object handle(EntityEventType type, IGameEntity owner,
			Object... args) {
		Collection<IEntityEventHandler> handlers = this.handlers.get(type);
		
		Object returnValue = null;
		
		for( IEntityEventHandler h : handlers ) {
			Object r = h.handle(type, owner, args);
			if( returnValue==null )
				returnValue = r; // returns only the first return value (questionable)
		}
		
		return returnValue;
	}

	@Override
	public boolean isHandled(EntityEventType type) {
		return handlers.containsKey(type);
	}

	@Override
	public Set<EntityEventType> getHandled() {
		return handlers.keySet();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject serialize() {		
		JSONObject obj = new JSONObject();
		
		JSONObject handlers = new JSONObject();
		for(EntityEventType type : this.handlers.keySet()) {
			Collection<IEntityEventHandler> entries = this.handlers.get(type);
			
			JSONArray jArray = new JSONArray();
			for( IEntityEventHandler h : entries ) 
				jArray.add(h.serialize());
			
			handlers.put(type.toString(), jArray);
		}
		
		obj.put("handlers", handlers);	
		obj.put("class", "CollectionEntityEventHandler");
		obj.put("uuid", uuid.toString());
		
		return obj;
	}

	@Override
	public IEntityEventHandler deserialize(JSONObject obj, IGameMap map) {
		uuid = UUID.fromString((String) obj.get("uuid"));
		IEntityEventHandler handler = map.getEventManager().get(uuid);
		if( handler != null )
			return handler;
		
		JSONObject handlers = (JSONObject) obj.get("handlers");
		
		for(Object o : handlers.keySet()) {
			EntityEventType type = EntityEventType.valueOf( (String) o );
			JSONArray jArray = (JSONArray) handlers.get(type.toString());
			
			for(Object object : jArray) {
				JSONObject jHandler = (JSONObject) object;
				IEntityEventHandler eventHandler = map.getEventManager().createEntityEventHandler((String) jHandler.get("class"));				
				eventHandler.deserialize(jHandler, map);
				map.getEventManager().add(eventHandler);
				this.handlers.put(type, eventHandler);
			}
		}
		
		map.getEventManager().add(this);
		
		return null;
	}

	@Override
	public UUID uuid() {
		return uuid;
	}

}
