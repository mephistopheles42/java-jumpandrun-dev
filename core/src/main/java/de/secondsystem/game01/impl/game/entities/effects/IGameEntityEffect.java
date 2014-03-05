package de.secondsystem.game01.impl.game.entities.effects;

import org.jsfml.graphics.RenderTarget;
import org.jsfml.system.Vector2f;

import de.secondsystem.game01.impl.map.IGameMap;
import de.secondsystem.game01.model.Attributes;
import de.secondsystem.game01.model.ISerializable;

public interface IGameEntityEffect extends ISerializable {

	void draw(RenderTarget rt, Vector2f position, float rotation);
	
	void onDestroy(IGameMap map);
	
}

interface IGameEntityEffectFactory {
	IGameEntityEffect create(IGameMap map, Attributes attributes, int worldMask, Vector2f position, float rotation);
}
