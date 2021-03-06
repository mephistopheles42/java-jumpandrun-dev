package de.secondsystem.game01.impl.game.entities.events;

import de.secondsystem.game01.impl.game.entities.IGameEntity;
import de.secondsystem.game01.impl.map.IGameMap;
import de.secondsystem.game01.impl.scripting.timer.TimerManager;
import de.secondsystem.game01.model.Attributes;
import de.secondsystem.game01.model.Attributes.Attribute;
import de.secondsystem.game01.model.Attributes.AttributeIf;

public class SpawnEventHandler implements IEventHandler {

	private final TimerManager timerManager;
	
	private final String type;
	
	private final Float relativeX, relativeY;
	
	private final Attributes attributes;
	
	public SpawnEventHandler(IGameMap map, Attributes attributes) {
		timerManager = map.getScriptEnv().getTimerManager();
		
		this.type = attributes.getString("type");
		this.attributes = attributes.getObject("attributes");
		this.relativeX = attributes.getFloat("relX", 0);
		this.relativeY = attributes.getFloat("relY", 0);
	}
	
	@Override
	public Object handle(Object... args) {
		final IGameEntity self = (IGameEntity) args[0];
		
		float sr = (float) Math.sin(Math.toRadians(self.getRotation() - (self.isFlippedHoriz()?180:0) ));
		float cr = (float) Math.cos(Math.toRadians(self.getRotation() - (self.isFlippedHoriz()?180:0) ));
		
		final float rx = relativeX*self.getWidth() * cr - relativeY*self.getHeight() * sr;
		final float ry = relativeX*self.getWidth() * sr - relativeY*self.getHeight() * cr;
		
		// delay creation (after physics-update)
		timerManager.createTimer(0, false, new Runnable() {
			@Override
			public void run() {
				self.manager().create(type, new Attributes(attributes, new Attributes(
						new AttributeIf(relativeX!=null, "x", rx+self.getPosition().x),
						new AttributeIf(relativeY!=null, "y", ry+self.getPosition().y),
						new AttributeIf(self.isFlippedHoriz(), "flipHoriz", true),
						new AttributeIf(self.isFlippedVert(), "flipVert", true),
						new Attribute("rotation", self.getRotation()),
						new Attribute("worldId", self.getWorldMask())
				)) );
			}
		});
		
		return null;
	}

	@Override
	public Attributes serialize() {
		return new Attributes(
				new Attribute(EventUtils.FACTORY, EventUtils.normalizeHandlerFactory(SpawnEHF.class.getName())),
				new Attribute("type", type),
				new Attribute("attributes", attributes),
				new AttributeIf(relativeX!=null, "relX", relativeX),
				new AttributeIf(relativeY!=null, "relY", relativeY)
		);
	}
	
}

final class SpawnEHF implements IEventHandlerFactory {
	@Override
	public SpawnEventHandler create(IGameMap map, Attributes attributes) {
		return new SpawnEventHandler(map, attributes);
	}
}
