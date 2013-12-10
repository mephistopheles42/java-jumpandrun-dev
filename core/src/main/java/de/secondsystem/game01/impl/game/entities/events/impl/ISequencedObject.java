package de.secondsystem.game01.impl.game.entities.events.impl;

import java.util.UUID;

import org.json.simple.JSONObject;

import de.secondsystem.game01.impl.game.entities.IGameEntity;
import de.secondsystem.game01.impl.game.entities.IGameEntityManager;
import de.secondsystem.game01.impl.game.entities.events.IEntityEventHandler.EntityEventType;

public interface ISequencedObject {
	Object handle(EntityEventType type, IGameEntity owner, Object... args);
	JSONObject serialize();

	/**
	 * @return Null if this ISequencedObject does not exist, returns the existing ISequencedObject otherwise.
	 */
	ISequencedObject deserialize(JSONObject obj, IGameEntityManager entityManager, SequenceManager sequenceManager);
	
	UUID uuid();
}
