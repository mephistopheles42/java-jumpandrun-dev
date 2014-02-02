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
import de.secondsystem.game01.impl.game.entities.IControllableGameEntity;
import de.secondsystem.game01.impl.game.entities.IGameEntity;
import de.secondsystem.game01.impl.game.entities.events.IEntityEventHandler.EntityEventType;
import de.secondsystem.game01.impl.game.entities.events.impl.KillEventHandler;
import de.secondsystem.game01.impl.game.entities.events.impl.PingPongEventHandler;
import de.secondsystem.game01.impl.intro.MainMenuState;
import de.secondsystem.game01.impl.map.GameMap;
import de.secondsystem.game01.impl.map.IGameMapSerializer;
import de.secondsystem.game01.impl.map.JsonGameMapSerializer;
import de.secondsystem.game01.impl.sound.MusicWrapper;
import de.secondsystem.game01.model.Attributes;
import de.secondsystem.game01.model.Attributes.Attribute;

public class MainGameState extends GameState {

	private final String mapId;
	
	private MusicWrapper backgroundMusic;
	
	private DevConsole console = new DevConsole();
	
	private GameMap map;
	
	private Camera camera;
	
	private IControllableGameEntity player;
	
	private KeyboardController controller;
	
	private final String PLAYER_UUID = "aa013690-1408-4a13-8329-cbfb1cfa7f6b";
	
	public MainGameState( String mapId ) {
		this.mapId = mapId;
	}
	
	private static final UUID PLAYER_DEATH_EVENT_UUID = UUID.nameUUIDFromBytes("playerDeath".getBytes());
	
	private final class PlayerDeathEventHandler extends KillEventHandler {

		public PlayerDeathEventHandler() {
			super(PLAYER_DEATH_EVENT_UUID, EntityEventType.DAMAGED);
		}
		
		@Override
		protected void killEntity(IGameEntity entity) {
			setNextState(new GameOverGameState());
		}
	}
	
	public class ScriptApi {
		private final GameContext ctx;
		public ScriptApi(GameContext ctx) {
			this.ctx = ctx;
		}
		
		public void loadMap(String mapId) {
			setNextState(new MainGameState(mapId));
		}
	}
	
	protected Object createScriptApi(GameContext ctx) {
		return new ScriptApi(ctx);
	}
	
	@Override
	protected void onStart(GameContext ctx) {
		if( backgroundMusic==null )
			backgroundMusic = new MusicWrapper(ctx.settings.volume);
		
		else
			backgroundMusic.play();
		
		if( map!=null ) {
			map.setFade(true);
			
		} else {
			IGameMapSerializer mapSerializer = new JsonGameMapSerializer();
			map = mapSerializer.deserialize(ctx, mapId, true, true);
			
			map.getEntityManager().create("lever", new Attributes(new Attribute("x",300), new Attribute("y",500), new Attribute("worldId",3)) )
			.addEventHandler(new PingPongEventHandler(UUID.randomUUID(), EntityEventType.USED, EntityEventType.DAMAGED));
		}
		
		map.getScriptEnv().bind("API", createScriptApi(ctx));
		
		player = (IControllableGameEntity) map.getEntityManager().get(UUID.fromString(PLAYER_UUID));
		if( player == null )
			player = (IControllableGameEntity) map.getEntityManager().create(UUID.fromString(PLAYER_UUID), "player", new Attributes(new Attribute("x",300), new Attribute("y",100)) );

		player.addEventHandler( new PlayerDeathEventHandler() );
		
		camera = new Camera(player);
			
//		// something like this will be implemented in the editor
//		IGameEntity entity = map.getEntityManager().create( "lever", new Attributes(new Attribute("x",210), new Attribute("y",270)) );
//		//IGameEntity explosion = map.getEntityManager().create( "explosion", new Attributes(new Attribute("x",50), new Attribute("y",-80)) );
//		IGameEntity fire1 = map.getEntityManager().create( "fire", new Attributes(new Attribute("x",-50), new Attribute("y",200)) );
//		IGameEntity fire2 = map.getEntityManager().create( "fire", new Attributes(new Attribute("x",-50), new Attribute("y",250)) );
//		IGameEntity fire3 = map.getEntityManager().create( "fire", new Attributes(new Attribute("x",-50), new Attribute("y",300)) );
//		
//		CollectionEntityEventHandler eventHandler = map.getEventManager().createCollectionEntityEventHandler();
//		AnimatedSequencedEntity animSequencedEntity = sequenceManager.createAnimatedSequencedEntity(entity);
//		Toggle toggle = sequenceManager.createToggle();
//		//toggle.inputOption.toggleTrigger.put(entity, animSequencedEntity);
//		toggle.addTarget(sequenceManager.createAnimatedSequencedEntity(fire1));
//		toggle.addTarget(sequenceManager.createAnimatedSequencedEntity(fire2));
//		toggle.addTarget(sequenceManager.createAnimatedSequencedEntity(fire3));
//		toggle.addTarget(animSequencedEntity);
//		IControllableGameEntity movingPlatform = map.getEntityManager().createControllable("moving platform", new Attributes(new Attribute("x",150), new Attribute("y",100)) );
//		PatrollingController movingPlatformCon = map.getControllerManager().createPatrollingController(movingPlatform, false);
//		movingPlatformCon.addTargetPoint(300, 100);
//		movingPlatformCon.addTargetPoint(150, 100);
//		movingPlatformCon.addTargetPoint(150, -100);
//		Condition isOwnerKinematic = sequenceManager.createCondition();
//		isOwnerKinematic.inTriggers.put(entity, animSequencedEntity);
//		isOwnerKinematic.add(toggle, isOwnerKinematic.outputOption.isOwnerKinematic, toggle.inputOption.toggleTriggers);
//		toggle.addTarget(sequenceManager.createControllableSequencedEntity(movingPlatformCon));
//		SequencedEntityEventHandler handler = map.getEventManager().createSequencedEntityEventHandler(EntityEventType.USED, isOwnerKinematic);
//		eventHandler.addEntityEventHandler(EntityEventType.USED, handler);	
//		entity.setEventHandler(eventHandler);
		
		
		console.setScriptEnvironment(map.getScriptEnv());
		
		controller = new KeyboardController(ctx.settings.keyMapping);
		controller.addGE(player);
	}

	@Override
	protected void onStop(GameContext ctx) {
		// TODO: free resources
		backgroundMusic.pause();
	}

	@Override
	protected void onFrame(GameContext ctx, long frameTime) {
		console.update(frameTime);
		
		controller.process();
		
		// update worlds
		map.update(frameTime);

		camera.update(frameTime);

		backgroundMusic.fade(map.getDefaultBgMusic(camera.getWorldId()), 5000);
		backgroundMusic.update(frameTime);
		
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
