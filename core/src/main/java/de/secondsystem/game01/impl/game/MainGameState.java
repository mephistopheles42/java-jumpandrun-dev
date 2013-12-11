package de.secondsystem.game01.impl.game;

import java.util.UUID;

import org.jsfml.graphics.ConstView;
import org.jsfml.window.Keyboard.Key;
import org.jsfml.window.event.Event;

import de.secondsystem.game01.impl.DevConsole;
import de.secondsystem.game01.impl.GameContext;
import de.secondsystem.game01.impl.GameState;
import de.secondsystem.game01.impl.editor.EditorGameState;
import de.secondsystem.game01.impl.game.controller.KeyboardController;
import de.secondsystem.game01.impl.game.controller.PatrollingController;
import de.secondsystem.game01.impl.game.entities.IControllableGameEntity;
import de.secondsystem.game01.impl.game.entities.IGameEntity;
import de.secondsystem.game01.impl.game.entities.events.CollectionEntityEventHandler;
import de.secondsystem.game01.impl.game.entities.events.IEntityEventHandler.EntityEventType;
import de.secondsystem.game01.impl.game.entities.events.SequencedEntityEventHandler;
import de.secondsystem.game01.impl.game.entities.events.impl.AnimatedSequencedEntity;
import de.secondsystem.game01.impl.game.entities.events.impl.Condition;
import de.secondsystem.game01.impl.game.entities.events.impl.ControllableSequencedEntity;
import de.secondsystem.game01.impl.game.entities.events.impl.SequenceManager;
import de.secondsystem.game01.impl.game.entities.events.impl.Toggle;
import de.secondsystem.game01.impl.intro.MainMenuState;
import de.secondsystem.game01.impl.map.GameMap;
import de.secondsystem.game01.impl.map.IGameMapSerializer;
import de.secondsystem.game01.impl.map.JsonGameMapSerializer;
import de.secondsystem.game01.model.Attributes;
import de.secondsystem.game01.model.Attributes.Attribute;

public class MainGameState extends GameState {

	private final DevConsole console = new DevConsole();
	
	private final GameMap map;
	
	private final Camera camera;
	
	private IControllableGameEntity player;
	
	private KeyboardController controller;
	
	private SequenceManager sequenceManager;
	
	private final String PLAYER_UUID = "aa013690-1408-4a13-8329-cbfb1cfa7f6b";
	
	public MainGameState( String mapId ) {
		IGameMapSerializer mapSerializer = new JsonGameMapSerializer();
		sequenceManager = new SequenceManager();
		
		map = mapSerializer.deserialize(mapId, true, true);
		
		player = (IControllableGameEntity) map.getEntityManager().get(UUID.fromString(PLAYER_UUID));
		if( player == null )
			player = (IControllableGameEntity) map.getEntityManager().create(UUID.fromString(PLAYER_UUID), "player", new Attributes(new Attribute("x",300), new Attribute("y",100)) );

		camera = new Camera(player);
		
			
//		// something like this will be implemented in the editor
//		IGameEntity entity = map.getEntityManager().create( "lever", new Attributes(new Attribute("x",210), new Attribute("y",270)) );
//		//IGameEntity explosion = map.getEntityManager().create( "explosion", new Attributes(new Attribute("x",50), new Attribute("y",-80)) );
//		IGameEntity fire1 = map.getEntityManager().create( "fire", new Attributes(new Attribute("x",-50), new Attribute("y",200)) );
//		IGameEntity fire2 = map.getEntityManager().create( "fire", new Attributes(new Attribute("x",-50), new Attribute("y",250)) );
//		IGameEntity fire3 = map.getEntityManager().create( "fire", new Attributes(new Attribute("x",-50), new Attribute("y",300)) );
//		if( entity.getEventHandler() instanceof CollectionEntityEventHandler ) {
//			CollectionEntityEventHandler eventHandler = (CollectionEntityEventHandler) entity.getEventHandler();
//			AnimatedSequencedEntity animSequencedEntity = sequenceManager.createAnimatedSequencedEntity(entity);
//			Toggle toggle = sequenceManager.createToggle();
//			//toggle.inputOption.toggleTrigger.put(entity, animSequencedEntity);
//			toggle.addTarget(sequenceManager.createAnimatedSequencedEntity(fire1));
//			toggle.addTarget(sequenceManager.createAnimatedSequencedEntity(fire2));
//			toggle.addTarget(sequenceManager.createAnimatedSequencedEntity(fire3));
//			toggle.addTarget(animSequencedEntity);
//			IControllableGameEntity movingPlatform = map.getEntityManager().createControllable("moving platform", new Attributes(new Attribute("x",150), new Attribute("y",100)) );
//			PatrollingController movingPlatformCon = map.getControllerManager().createPatrollingController(movingPlatform, false);
//			movingPlatformCon.addTargetPoint(300, 100);
//			movingPlatformCon.addTargetPoint(150, 100);
//			movingPlatformCon.addTargetPoint(150, -100);
//			Condition isOwnerKinematic = sequenceManager.createCondition();
//			isOwnerKinematic.inTriggers.put(entity, animSequencedEntity);
//			isOwnerKinematic.add(toggle, isOwnerKinematic.outputOption.isOwnerKinematic, toggle.inputOption.toggleTriggers);
//			toggle.addTarget(sequenceManager.createControllableSequencedEntity(movingPlatformCon));
//			SequencedEntityEventHandler handler = new SequencedEntityEventHandler(EntityEventType.USED, isOwnerKinematic);
//			eventHandler.addEntityEventHandler(EntityEventType.USED, handler);	
//			
//			//isOwnerKinematic.serialize();
//			
//		}
		
		console.setScriptEnvironment(map.getScriptEnv());
	}
	
	@Override
	protected void onStart(GameContext ctx) {
		controller = new KeyboardController(ctx.settings.keyMapping);
		controller.addGE(player);
	}

	@Override
	protected void onStop(GameContext ctx) {
		// TODO: free resources
	}

	@Override
	protected void onFrame(GameContext ctx, long frameTime) {
		console.update(frameTime);
		
		controller.process();
		
		// update worlds
		map.update(frameTime);

		camera.update(frameTime);
		
		final ConstView cView = ctx.window.getView();
		ctx.window.setView(camera.createView(cView));
		map.setActiveWorldId(camera.getWorldId());
		
		// drawing
		map.draw(ctx.window);
		
		ctx.window.setView(cView);
		
		
		// events
		for(Event event : ctx.window.pollEvents()) {
	        if(event.type == Event.Type.CLOSED) {
	            //The user pressed the close button
	            ctx.window.close();
	            
	        } else if( event.type==Event.Type.KEY_RELEASED ) {
	        	if( event.asKeyEvent().key==Key.F12 ) {
	        		setNextState(new EditorGameState(this, map));
	        	}
	        	if( event.asKeyEvent().key==Key.ESCAPE ) {
	        		setNextState(new MainMenuState(this));
	        	}
	        }
	        
	        controller.processEvents(event);
	    }
	}

}
