package de.secondsystem.game01.impl.game.entities;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.script.ScriptException;

import org.jsfml.graphics.Color;
import org.jsfml.graphics.IntRect;
import org.jsfml.graphics.RenderTarget;
import org.jsfml.graphics.RenderTexture;
import org.jsfml.graphics.Texture;
import org.jsfml.graphics.TextureCreationException;
import org.jsfml.graphics.View;
import org.jsfml.system.Vector2f;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.secondsystem.game01.impl.gui.ThumbnailButton.ThumbnailData;
import de.secondsystem.game01.impl.map.IGameMap;
import de.secondsystem.game01.impl.map.IGameMap.WorldId;
import de.secondsystem.game01.model.Attributes;
import de.secondsystem.game01.model.Attributes.Attribute;
import de.secondsystem.game01.model.GameException;
import de.secondsystem.game01.model.collections.LruCache;

public final class GameEntityManager implements IGameEntityManager {

	private static final Path ARCHETYPE_PATH = Paths.get("assets", "entities");
	
	private final List<String> archetypes = Collections.unmodifiableList( new ArrayList<String>(Arrays.asList(ARCHETYPE_PATH.toFile().list())) );
	
	private final boolean overrideOptionalCreation;
	
	private final Map<UUID, IGameEntity> entities = new HashMap<>();
	@SuppressWarnings("unchecked")
	private final List<IGameEntity>[] orderedEntities = new ArrayList[256];
	private final List<Byte> orderedEntitiesKeys = new ArrayList<>(5);
	private final Map<String, Set<IGameEntity>> entityGroups = new HashMap<>(4);
	
	private final Set<UUID> entitiesToDestroy = new HashSet<>();
	private final Set<IGameEntity> entitiesToAdd = new HashSet<>();
	
	private static final LruCache<String, EntityArchetype> ARCHETYPE_CACHE = new LruCache<>(100, new ArchetypeLoader());
	
	final IGameMap map;
	
	public GameEntityManager(IGameMap map, boolean overrideOptionalCreation) {
		this.map = map;
		this.overrideOptionalCreation = overrideOptionalCreation;
	}
	
	@Override
	public List<String> listArchetypes() {
		return archetypes;
	}
	
	@SuppressWarnings("unchecked")
	private List<ThumbnailData> thumbnailDatas[] = new ArrayList[WorldId.values().length];;
	
	@Override
	public List<ThumbnailData> generateThumbnails(WorldId currentWorld) {
		if( thumbnailDatas[currentWorld.ordinal()]!=null )
			return thumbnailDatas[currentWorld.ordinal()];
		
		List<ThumbnailData> td = new ArrayList<ThumbnailData>();
		
		UUID uuid = UUID.randomUUID();
		try {
			long t = System.currentTimeMillis();
			RenderTexture texture = new RenderTexture();
			texture.create(100, 100);
			
			for( String a : listArchetypes() ) {
				EntityArchetype at = ARCHETYPE_CACHE.get(a);
				
				if( at!=null ) {
					IGameEntity e = at.create(uuid, this, new Attributes(new Attribute("x", 0), new Attribute("y", 0)));
					float size=Math.max(e.getWidth(), e.getHeight());
					texture.setView(new View(e.getPosition(), new Vector2f(size,size)));
					texture.clear(Color.BLACK);
					e.update(2000);
					e.draw(texture, currentWorld);
					texture.display();

					// store and load texture to fix weird SFML bug (RenderTexture turn blank after a while)
					Path p = Paths.get("assets", "tmp", a+".bmp");
					try {
						texture.getTexture().copyToImage().saveToFile(p);
					} catch (IOException e1) {
						e1.printStackTrace();
					}					
					Texture tex = new Texture();
					tex.loadFromFile(p);
					Files.deleteIfExists(p);
					
					td.add(new ThumbnailData(a, tex, new IntRect(0, 0, 100, 100)));
					
					e.onDestroy();
				}
			}
			
			System.out.println("Time: "+(System.currentTimeMillis()-t));
			
		} catch (TextureCreationException | IOException e) {
			throw new GameException(e);
		}
		
		return thumbnailDatas[currentWorld.ordinal()]=td;
	}
	
	@Override
	public IControllableGameEntity createControllable( String type, Map<String, Object> args ) {
		return (IControllableGameEntity) create(type, args);
	}

	@Override
	public IGameEntity create(String type, Map<String, Object> attr) {
		return create(null, type, attr);
	}
	
	@Override
	public IGameEntity create(UUID uuid, String type, Map<String, Object> attr) {
		if( overrideOptionalCreation ) {
			final Object createCondition = attr.get("createIf");
			if( createCondition!=null )
				try {
					if( !Boolean.valueOf(map.getScriptEnv().eval(createCondition.toString()).toString()) )
						return null;
					
				} catch (ScriptException e) {
					e.printStackTrace();
				}
		}
		
		if( uuid==null )
			uuid = UUID.randomUUID();
			
		EntityArchetype at = ARCHETYPE_CACHE.get(type);
		
		if( at==null )
			throw new EntityCreationException("Unknown archetype '"+type+"' for entity: "+uuid);
		
		IGameEntity e = at.create(uuid, this, attr);
	
		entitiesToAdd.add(e);
		return e;
	}
	
	@Override
	public void destroy( UUID eId ) {
		entitiesToDestroy.add(eId);
	}
	
	private void destroyAndAddEntites() {
		for( IGameEntity e : entitiesToAdd ) {
			if( entities.put(e.uuid(), e)==null ) {
				int oId = e.orderId()+128;
				
				List<IGameEntity> sg = orderedEntities[oId];
				if( sg==null ) {
					sg = new ArrayList<>();
					orderedEntities[oId] = sg;
					orderedEntitiesKeys.add(e.orderId());
					Collections.sort(orderedEntitiesKeys);
				}
				
				sg.add(e);
				
				if( e.group()!=null && !e.group().isEmpty() ) {
					Set<IGameEntity> ges = entityGroups.get(e.group());
					if( ges==null )
						entityGroups.put(e.group(), ges=new HashSet<>());
					
					ges.add(e);
				}
			}
		}
		entitiesToAdd.clear();
		
		for( UUID eId : entitiesToDestroy ) {
			IGameEntity entity = entities.get(eId);
			if( entity!=null ) {
				entity.onDestroy();
				entities.remove(eId);
				List<IGameEntity> sg = orderedEntities[entity.orderId()+128];
				if( sg!=null ) {
					sg.remove(entity);
				}

				if( entity.group()!=null && !entity.group().isEmpty() ) {
					Set<IGameEntity> ges = entityGroups.get(entity.group());
					if( ges!=null )
						ges.remove(entity);
				}
			}
		}
		entitiesToDestroy.clear();
	}

	@Override
	public IGameEntity get( UUID eId ) {
		return entities.get(eId);
	}
	@Override
	public IWeakGameEntityRef getRef(final UUID eId) {
		return new IWeakGameEntityRef() { 
			private IGameEntity entity;
			@Override public UUID uuid() {
				return eId;
			}
			
			@Override public IGameEntityManager manager() {
				return GameEntityManager.this;
			}
			
			@Override public IGameEntity get() {
				if( entity!=null )	return entity;
				return entity= GameEntityManager.this.get(eId);
			}
		};
	}

	@Override
	public void draw(final WorldId worldId, final RenderTarget rt) {
		destroyAndAddEntites();
		
		for( Byte orderId : orderedEntitiesKeys )
			for( IGameEntity entity : orderedEntities[orderId+128] )
				if( entity.isInWorld(worldId) )
					entity.draw(rt, worldId);
	}

	@Override
	public void update(long frameTimeMs) {
		for( IGameEntity entity : entities.values() )
			entity.update(frameTimeMs);
	}
	
	private static final class EntityArchetype {
		public final String archetype;
		public final Map<String, Object> attributes;
		
		public EntityArchetype(String archetype, Map<String, Object> attributes) throws EntityCreationException {
			this.archetype = archetype;
			this.attributes = attributes;
		}
		
		public IGameEntity create(UUID uuid, GameEntityManager em, Map<String, Object> attr) {
			final Attributes inAttr = new Attributes( attributes, attr);
			
			IGameEntity entity = inAttr.getBoolean("controllable",false) ? 
					new ControllableGameEntity(uuid, em, em.map, inAttr)
					: new GameEntity(uuid, em, em.map, inAttr);
					
			entity.setEditableState(new EditableEntityStateImpl(this, new Attributes(attr)) );
			return entity;
		}
	}
	
	private static final class ArchetypeLoader implements LruCache.Loader<String, EntityArchetype> {
		
		private JSONParser parser = new JSONParser();
		
		@SuppressWarnings("unchecked")
		@Override
		public synchronized EntityArchetype load(String key) {
			try ( Reader reader = Files.newBufferedReader(ARCHETYPE_PATH.resolve(key), StandardCharsets.UTF_8) ){
				JSONObject obj = (JSONObject) parser.parse(reader);
				
				return new EntityArchetype(key, Collections.unmodifiableMap(obj));
				
			} catch (IOException | ParseException e) {
				System.err.println("Unable to load entity archetype: "+e.getMessage());
				e.printStackTrace();
				return null;
			}
		}
		
	}

	private static final class EditableEntityStateImpl implements IEditableEntityState {

		private final EntityArchetype archetype;
		
		private final Attributes attributes;
		
		EditableEntityStateImpl(EntityArchetype archetype, Attributes attributes) {
			this.archetype = archetype;
			this.attributes = attributes;
		}
		
		@Override
		public String getArchetype() {
			return archetype.archetype;
		}

		@Override
		public Attributes getAttributes() {
			return new Attributes(attributes, archetype.attributes);
		}
	}
	
	@Override
	public Attributes serialize() {	
		final List<Attributes> entityAttributes = new ArrayList<>(entities.size());
		for(IGameEntity entity : entities.values())
			entityAttributes.add( filterEntityAttributes(entity.serialize(), entity.getEditableState().getArchetype()) );
		
		return new Attributes(
				new Attribute("entities", entityAttributes)
		); 
	}
	
	private static final Attributes filterEntityAttributes( Attributes attributes, String archetype ) {
		EntityArchetype at = ARCHETYPE_CACHE.get(archetype);
		
		if( at!=null ) {
			for( Entry<String, Object> e : at.attributes.entrySet() ) {
				Object o1 = e.getValue();
				Object o2 = attributes.get(e.getKey());
				
				if( compObject(o1, o2) )
					attributes.remove(e.getKey());
			}
		}
	
		return attributes;
	}
	
	private static boolean compObject(Object o1, Object o2) {
		return o1.equals(o2) 
				|| (o1 instanceof Number && o2 instanceof Number && !o1.getClass().equals(o2.getClass()) && compNumber(o1,o2))
				|| (o1 instanceof Collection && o2 instanceof Collection && compCollection(o1,o2)) 
				|| (o1 instanceof Map && o2 instanceof Map && compMap(o1,o2));
	}
	
	private static boolean compNumber(Object o1, Object o2) {
		if( o1 instanceof Double || o1 instanceof Float || o2 instanceof Double || o2 instanceof Float )
			return ((Number) o1).doubleValue()==((Number)o2).doubleValue();
		
		else
			return ((Number) o1).longValue()==((Number)o2).longValue();
	}

	@SuppressWarnings("unchecked")
	private static boolean compMap(Object o1, Object o2) {
		Map<Object, Object> m1 = (Map<Object, Object>) o1;
		Map<Object, Object> m2 = (Map<Object, Object>) o2;
		
		if( m1.size()!=m2.size() )
			return false;

		for( Entry<Object, Object> e : m1.entrySet() ) {
			Object so1 = e.getValue();
			Object so2 = m2.get(e.getKey());
			
			if( !compObject(so1, so2) )
				return false;
		}
		
		return true;
	}

	@SuppressWarnings("unchecked")
	private static boolean compCollection(Object o1, Object o2) {
		Collection<Object> c1 = (Collection<Object>) o1;
		Collection<Object> c2 = (Collection<Object>) o2;
		
		outer: for( Object so1 : c1 ) {
			for( Object so2 : c2 )
				if( compObject(so1, so2) )
					continue outer;
			
			return false;
		}
		
		return true;
	}

	@Override
	public void deserialize(Attributes attributes) {
		if( attributes==null )
			return;
		
		final List<Attributes> entityAttributes = attributes.getObjectList("entities");
		
		if( entityAttributes==null )
			return;
		
		for(Attributes entityAttr : entityAttributes) {
			final UUID uuid = UUID.fromString( entityAttr.getString("uuid") );
			final String archetype = entityAttr.getString("archetype");
			
			create(uuid, archetype, entityAttr);
		}
		
		destroyAndAddEntites();
	}
	
	@Override
	public List<IGameEntity> findEntities(WorldId worldId, Vector2f point) {
		List<IGameEntity> r = new ArrayList<>();
		for(IGameEntity entity : entities.values())
			if( entity.isInWorld(worldId) && entity.inside(point) )
				r.add(entity);
		
		return r;
	}

	@Override
	public Set<IGameEntity> listByGroup(String group) {
		Set<IGameEntity> r = entityGroups.get(group);
		if( r==null )
			entityGroups.put(group, r=new HashSet<>());
		return Collections.unmodifiableSet(r);
	}
	
}
