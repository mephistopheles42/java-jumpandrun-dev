package de.secondsystem.game01.impl.game.entities.events.impl;

import java.util.HashMap;
import java.util.UUID;

import org.json.simple.JSONObject;

import de.secondsystem.game01.impl.game.entities.IGameEntity;
import de.secondsystem.game01.impl.game.entities.IGameEntityManager;
import de.secondsystem.game01.impl.game.entities.events.IEntityEventHandler.EntityEventType;

public class Toggle extends SequencedObject {

	public class ToggleInputOption {
		public final HashMap<IGameEntity, IToggled> onTriggers     = new HashMap<>();
		public final HashMap<IGameEntity, IToggled> offTriggers    = new HashMap<>(); 
		public final HashMap<IGameEntity, IToggled> toggleTriggers = new HashMap<>();
	}
	
	public final ToggleInputOption inputOption = new ToggleInputOption();	
	
	public Toggle(UUID uuid) {
		super(uuid);
	}
	
	@Override
	public Object handle(EntityEventType type, IGameEntity owner, Object... args) {
		super.handle(type, owner, args);
		
		if( inputOption.onTriggers.get(owner) != null ) 
			for( IToggled target : targets )
				target.onTurnOn();
		
		if( inputOption.offTriggers.get(owner) != null ) 
			for( IToggled target : targets )
				target.onTurnOff();
		
		if( inputOption.toggleTriggers.get(owner) != null ) 
			for( IToggled target : targets )
				target.onToggle();
		
		return null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject serialize() {
		JSONObject obj = super.serialize();
		
		obj.put("onTriggers", serializeTriggers(inputOption.onTriggers));
		obj.put("offTriggers", serializeTriggers(inputOption.offTriggers));
		obj.put("toggleTriggers", serializeTriggers(inputOption.toggleTriggers));
		
		return obj;
	}
	
	@Override
	public ISequencedObject deserialize(JSONObject obj, IGameEntityManager entityManager, SequenceManager sequenceManager) {
		ISequencedObject seqObj = super.deserialize(obj, entityManager, sequenceManager);
		if( seqObj != null )
			return seqObj;
		
		deserializeTriggers(inputOption.onTriggers, obj, entityManager, "onTriggers", sequenceManager);
		deserializeTriggers(inputOption.offTriggers, obj, entityManager, "offTriggers", sequenceManager);
		deserializeTriggers(inputOption.toggleTriggers, obj, entityManager, "toggleTriggers", sequenceManager);
		
		return null;
	}
}
