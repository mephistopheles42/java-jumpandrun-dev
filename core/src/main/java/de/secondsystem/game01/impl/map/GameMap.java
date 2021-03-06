package de.secondsystem.game01.impl.map;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsfml.graphics.Color;
import org.jsfml.graphics.ConstView;
import org.jsfml.graphics.RenderTarget;
import org.jsfml.graphics.View;
import org.jsfml.system.Vector2f;

import de.secondsystem.game01.impl.GameContext;
import de.secondsystem.game01.impl.game.entities.GameEntityManager;
import de.secondsystem.game01.impl.game.entities.IGameEntityManager;
import de.secondsystem.game01.impl.graphic.LightMap;
import de.secondsystem.game01.impl.map.objects.EntityLayer;
import de.secondsystem.game01.impl.map.objects.LayerObjectType;
import de.secondsystem.game01.impl.map.objects.LightLayer;
import de.secondsystem.game01.impl.map.objects.SimpleLayer;
import de.secondsystem.game01.impl.map.physics.Box2dPhysicalWorld;
import de.secondsystem.game01.impl.map.physics.IPhysicsWorld;
import de.secondsystem.game01.impl.scripting.IScriptApi;
import de.secondsystem.game01.impl.scripting.ScriptEnvironment;
import de.secondsystem.game01.impl.scripting.ScriptEnvironment.ScriptType;
import de.secondsystem.game01.model.Attributes;
import de.secondsystem.game01.model.Attributes.Attribute;

public class GameMap implements IGameMap {
	
	public static final class GameWorld {
		final ILayer[] graphicLayer;
		Color backgroundColor;
		Color ambientLight;
		String backgroundMusic;
		
		GameWorld( WorldId worldId, IGameEntityManager entityManager, LightMap lightMap ) {
			graphicLayer = new ILayer[LayerType.LAYER_COUNT];
			for( LayerType l : LayerType.values() )
				switch( l ) {
					case OBJECTS:
						graphicLayer[l.layerIndex] = new EntityLayer(l, entityManager, worldId);
						break;
						
					case LIGHTS:
						graphicLayer[l.layerIndex] = new LightLayer(worldId, l, lightMap);
						break;
						
					default:
						graphicLayer[l.layerIndex] = new SimpleLayer(l, worldId);
				}
			
			backgroundColor = Color.BLACK;
			ambientLight = Color.WHITE;
		}
		void addNode( LayerType layer, ILayerObject sprite ) {
			addNode(layer.layerIndex, sprite);
		}
		void addNode( int layerIndex, ILayerObject sprite ) {
			graphicLayer[layerIndex].addNode(sprite);
		}
	}
	
	private String mapId;
	
	final GameWorld gameWorld[] = new GameWorld[2];
	
	private Tileset tileset;
	
	private WorldId activeWorldId;
	
	public final boolean editable;
	
	public final boolean playable;
	
	private final IPhysicsWorld physicalWorld;
	
	private final IGameEntityManager entityManager;
	
	private final Set<IWorldSwitchListener> worldSwitchListeners = new HashSet<>();
	
	private final LightMap lightMap;
	
	final ScriptEnvironment scripts;
	
	private boolean renderLightmap = true;
	
	public GameMap(GameContext ctx, String mapId, Tileset tileset, IScriptApi scriptApi) {
		this(ctx, mapId, tileset, scriptApi, true, true);
	}
	
	GameMap(GameContext ctx, String mapId, Tileset tileset, IScriptApi scriptApi, boolean playable, boolean editable) {
		this.mapId = mapId;
		this.tileset = tileset;
		this.editable = editable;
		this.playable = playable;
		this.activeWorldId = WorldId.MAIN;

		lightMap = ctx!=null && ctx.settings.dynamicLight ? new LightMap(
				ctx.window, 
				(byte) WorldId.values().length, 
				new Vector2f(ctx.settings.width, ctx.settings.height), 
				ctx.getViewWidth(), 
				ctx.getViewHeight()) : null;
		
		physicalWorld = new Box2dPhysicalWorld();
		physicalWorld.init(new Vector2f(0, 15.f));
		
		entityManager = new GameEntityManager(this, !playable);	

		for( WorldId wId : WorldId.values() )
			gameWorld[wId.arrayIndex] = new GameWorld(wId, entityManager, lightMap);
		
		scripts = new ScriptEnvironment(ScriptType.JAVA_SCRIPT, scriptApi,
				new Attributes(
					new Attribute("mapId", mapId), 
					new Attribute("map", this), 
					new Attribute("entities", entityManager)) );
	}
		
	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.map.IGameMap#getMapId()
	 */
	@Override
	public String getMapId() {
		return mapId;
	}
	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.map.IGameMap#getTileset()
	 */
	@Override
	public Tileset getTileset() {
		return tileset;
	}
	
	@Override
	public void toogleLightmap() {
		renderLightmap=!renderLightmap;
	}
	@Override
	public void setAmbientLight(Color color) {
		gameWorld[activeWorldId.arrayIndex].ambientLight = color;
	}
	@Override
	public void setBackgroundColor(Color color) {
		gameWorld[activeWorldId.arrayIndex].backgroundColor = color;
	}
	
	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.map.IGameMap#switchWorlds()
	 */
	@Override
	public void switchWorlds() {
		activeWorldId = activeWorldId==WorldId.MAIN ? WorldId.OTHER : WorldId.MAIN;
		
		for( IWorldSwitchListener listener : worldSwitchListeners )
			listener.onWorldSwitch(activeWorldId);
	}
	
	protected void preDraw(RenderTarget rt) {
		rt.clear(gameWorld[activeWorldId.arrayIndex].backgroundColor);
		
		lightMap.setAmbientLight(gameWorld[activeWorldId.arrayIndex].ambientLight);
	}
	protected void postDraw(RenderTarget rt) {
	}
	
	protected final ConstView calcLayerView(float parallax, ConstView baseView) {
		return parallax!=1.f ? new View(Vector2f.mul(baseView.getCenter(), parallax), baseView.getSize()) : baseView;
	}
	protected void drawVisibleLayer(RenderTarget target, ConstView baseView, WorldId worldId, LayerType layerType) {
		ILayer layer = gameWorld[worldId.arrayIndex].graphicLayer[layerType.layerIndex];
		
		if( layer.isVisible() ) {
			target.setView(calcLayerView(layerType.parallax, baseView));
			 
			layer.draw(target, worldId);
			target.setView(baseView);
		}
	}
	
	@Override
	public void drawInactiveWorld(RenderTarget rt) {
		
		final ConstView cView = rt.getView();
		
		final WorldId inactiveWorld = activeWorldId==WorldId.MAIN ? WorldId.OTHER : WorldId.MAIN;
		
		rt.clear(Color.TRANSPARENT);
		
		for( LayerType l : LayerType.values() )
			if( l.fade )
				drawVisibleLayer(rt, cView, inactiveWorld, l);

		rt.setView(cView);
	}
	
	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.map.IGameMap#draw(org.jsfml.graphics.RenderTarget)
	 */
	@Override
	public void draw(RenderTarget rt) {
		if( lightMap!=null && renderLightmap ) {
			lightMap.setTarget(rt);
			lightMap.setView(rt.getView());
			rt = lightMap;
		}

		preDraw(rt);
		
		for( LayerType l : LayerType.values() )
			drawVisibleLayer(rt, rt.getView(), activeWorldId, l);
		
		postDraw(rt);
	}
	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.map.IGameMap#update(long)
	 */
	@Override
	public void update(long frameTimeMs) {
		if( playable )
			scripts.update(frameTimeMs);
		
		for( LayerType l : LayerType.values() )
			if( l.updated )
				for( GameWorld world : gameWorld )
					world.graphicLayer[l.layerIndex].update(frameTimeMs);
		
		entityManager.update(frameTimeMs);
		
		if( physicalWorld !=null )
			physicalWorld.update(frameTimeMs);
	}
	
	
	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.map.IGameMap#getActiveWorldId()
	 */
	@Override
	public WorldId getActiveWorldId() {
		return activeWorldId;
	}

	@Override
	public void setActiveWorldId(WorldId worldId) {
		if( activeWorldId!=worldId )
			switchWorlds();
	}

	@Override
	public ILayerObject createNode(LayerObjectType type, Attributes attributes) {
		return type.create(this, attributes);
	}
	
	@Override
	public ILayerObject updateNode(ILayerObject obj, Attributes attributes) {
		LayerType layer = obj.getLayerType();
		
		ILayerObject nObj = createNode(obj.typeUuid(), attributes);

		for( GameWorld world : gameWorld )
			world.graphicLayer[layer.layerIndex].replaceNode(obj, nObj);
		
		return nObj;
	}
	
	@Override
	public void replaceNode(ILayerObject obj, ILayerObject newObj) {
		for( GameWorld world : gameWorld )
			world.graphicLayer[obj.getLayerType().layerIndex].replaceNode(obj, newObj);
	}
	
	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.map.IGameMap#addNode(int, de.secondsystem.game01.impl.map.LayerType, de.secondsystem.game01.impl.map.LayerObject)
	 */
	@Override
	public void addNode( WorldId worldId, LayerType layer, ILayerObject sprite ) {
		gameWorld[worldId.arrayIndex].graphicLayer[layer.layerIndex].addNode(sprite);
	}
	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.map.IGameMap#addNode(de.secondsystem.game01.impl.map.LayerType, de.secondsystem.game01.impl.map.LayerObject)
	 */
	@Override
	public void addNode( LayerType layer, ILayerObject sprite ) {
		addNode(activeWorldId, layer, sprite);
	}
	@Override
	public List<ILayerObject> findNodes(LayerType layer, Vector2f point) {
		return gameWorld[activeWorldId.arrayIndex].graphicLayer[layer.layerIndex].findNodes(point);
	}
	
	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.map.IGameMap#remove(de.secondsystem.game01.impl.map.LayerType, de.secondsystem.game01.impl.map.LayerObject)
	 */
	@Override
	public void remove( LayerType layer, ILayerObject s ) {
		gameWorld[activeWorldId.arrayIndex].graphicLayer[layer.layerIndex].remove(s);
	}
		
	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.map.IGameMap#flipShowLayer(de.secondsystem.game01.impl.map.LayerType)
	 */
	@Override
	public boolean flipShowLayer( LayerType layer ) {
		return gameWorld[activeWorldId.arrayIndex].graphicLayer[layer.layerIndex].setVisible( !gameWorld[activeWorldId.arrayIndex].graphicLayer[layer.layerIndex].isVisible() );
	}
	
	@Override
	public boolean isLayerShown(LayerType layer) {
		return gameWorld[activeWorldId.arrayIndex].graphicLayer[layer.layerIndex].isVisible();
	}
	@Override
	public void setShowLayer(LayerType layer, boolean show) {
		gameWorld[activeWorldId.arrayIndex].graphicLayer[layer.layerIndex].setVisible(show);
	}

	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.map.IGameMap#getPhysicalWorld()
	 */
	@Override
	public IPhysicsWorld getPhysicalWorld() {
		return physicalWorld;
	}

	@Override
	public void registerWorldSwitchListener(IWorldSwitchListener listener) {
		worldSwitchListeners.add(listener);
	}

	@Override
	public void deregisterWorldSwitchListener(IWorldSwitchListener listener) {
		worldSwitchListeners.remove(listener);
	}

	@Override
	public boolean isEditable() {
		return editable;
	}

	@Override
	public IGameEntityManager getEntityManager() {
		return entityManager;
	}

	@Override
	public ScriptEnvironment getScriptEnv() {
		return scripts;
	}

	@Override
	public LightMap getLightMap() {
		return lightMap;
	}

	public String getDefaultBgMusic(WorldId world) {
		return gameWorld[world.arrayIndex].backgroundMusic;
	}
	
}
