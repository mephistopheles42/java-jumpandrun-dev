package de.secondsystem.game01.impl.game;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.jsfml.audio.Sound;
import org.jsfml.graphics.Color;
import org.jsfml.graphics.RenderTarget;
import org.jsfml.graphics.Sprite;
import org.jsfml.system.Vector2f;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.secondsystem.game01.impl.GameContext;
import de.secondsystem.game01.impl.ResourceManager;
import de.secondsystem.game01.impl.scripting.IScriptApi;
import de.secondsystem.game01.impl.sound.MonologueTextBox;
import de.secondsystem.game01.model.GameException;
import de.secondsystem.game01.model.IDrawable;
import de.secondsystem.game01.model.IUpdateable;

public class ScriptApiImpl implements IScriptApi, IDrawable, IUpdateable {
	/**
	 * 
	 */
	private final MainGameState mainGameState;
	private final GameContext ctx;
	private JSONObject storedValues;
	private final JSONParser parser = new JSONParser();

	private ThreadedMapLoader activeMapLoader;
	private final Set<Sprite> sprites = new HashSet<>();
	private final MonologueTextBox monologueTextBox;
	
	public ScriptApiImpl(MainGameState mainGameState, GameContext ctx) {
		this.mainGameState = mainGameState;
		this.ctx = ctx;
		try ( Reader reader = Files.newBufferedReader(Paths.get("save.json"), StandardCharsets.UTF_8) ){
			storedValues = (JSONObject) parser.parse(reader);
			
		} catch (IOException | ParseException e) {
			storedValues = new JSONObject();
		}

		try {
			monologueTextBox = new MonologueTextBox(ResourceManager.font.get("FreeSans.otf"), 30, new Vector2f(ctx.getViewWidth()/2, ctx.getViewHeight()-100));
			
		} catch (GameException e) {
			throw new GameException("Unable to load font for MonologueTextBox: "+e.getMessage(), e);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.game.IScriptApi#loadMap(java.lang.String)
	 */
	@Override
	public void loadMap(String mapId) {
		System.out.println("load:" +mapId);
		if( activeMapLoader==null )
			activeMapLoader = new ThreadedMapLoader(mapId, ctx);
	}
	
	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.game.IScriptApi#playMonologue(java.lang.String)
	 */
	@Override
	public void playMonologue(String name) {
		monologueTextBox.play(name);
	}
	
	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.game.IScriptApi#store(java.lang.String, java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void store(String key, Object val) {
		storedValues.put(key, val);
		
		try ( Writer writer = Files.newBufferedWriter(Paths.get("save.json"), StandardCharsets.UTF_8) ){
			storedValues.writeJSONString(writer);
			
		} catch (IOException e) {
			System.err.println("Unable to store value: "+e.getMessage());
		}
	}
	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.game.IScriptApi#load(java.lang.String)
	 */
	@Override
	public Object load(String key) {
		return storedValues.get(key);
	}
	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.game.IScriptApi#exists(java.lang.String)
	 */
	@Override
	public boolean exists(String key) {
		return storedValues.containsKey(key);
	}
	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.game.IScriptApi#playSound(java.lang.String)
	 */
	@Override
	public Sound playSound(String name) {
		try {
			Sound s = new Sound(ResourceManager.sound.get(name));
			s.setRelativeToListener(false);
			s.play();
			return s;
			
		} catch (GameException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.game.IScriptApi#createSprite(java.lang.String, float, float)
	 */
	@Override
	public Sprite createSprite(String texture, float x, float y) {
		try {
			Sprite s = new Sprite(ResourceManager.texture_gui.get(texture).texture);
			s.setPosition(x, y);
			this.sprites.add(s);
			return s;
			
		} catch (GameException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.game.IScriptApi#deleteSprite(org.jsfml.graphics.Sprite)
	 */
	@Override
	public void deleteSprite(Sprite sprite) {
		this.sprites.remove(sprite);
	}
	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.game.IScriptApi#updateSpriteTex(org.jsfml.graphics.Sprite, java.lang.String)
	 */
	@Override
	public void updateSpriteTex(Sprite sprite, String texture) {
		try {
			sprite.setTexture(ResourceManager.texture_gui.get(texture).texture);
			
		} catch (GameException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}
	/* (non-Javadoc)
	 * @see de.secondsystem.game01.impl.game.IScriptApi#updateSpriteColor(org.jsfml.graphics.Sprite, int, int, int, int)
	 */
	@Override
	public void updateSpriteColor(Sprite sprite, int r, int g, int b, int a) {
		sprite.setColor(new Color(r, g, b, a));
	}

	@Override
	public void update(long frameTime) {
		if( activeMapLoader!=null ) {
			activeMapLoader.update(frameTime);
		}
		
		monologueTextBox.update(frameTime);
	}

	@Override
	public void draw(RenderTarget renderTarget) {
		for( Sprite s : sprites )
			ctx.window.draw(s);
		

		if( activeMapLoader!=null ) {
			activeMapLoader.draw(renderTarget);
			
			if( activeMapLoader.isFinished() )
				mainGameState.requestStateSwitch(activeMapLoader.getLoadedMap());
		}
		
		monologueTextBox.draw(renderTarget);
	}
}