package de.secondsystem.game01.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jsfml.graphics.RenderWindow;
import org.jsfml.graphics.View;
import org.jsfml.system.Vector2f;
import org.jsfml.window.ContextSettings;
import org.jsfml.window.Mouse;
import org.jsfml.window.VideoMode;
import org.jsfml.window.WindowStyle;

import de.secondsystem.game01.fsm.IContext;
import de.secondsystem.game01.model.Settings;

/**
 * GameState independent resources (stuff used by most/all GameStates)
 * @author lowkey
 *
 */
public class GameContext implements IContext {

	private static final String WINDOW_TITLE = "GAME_01";
	
	private static final int VIEW_HEIGHT = 1080;
	
	public static final Path CONFIG_PATH = Paths.get("game-cfg.properties");
	
	private int viewWidth;
	
	public final RenderWindow window;
	
	public final Settings settings;
	
	
	/**
	 * Creates a new window and initializes resources required by most/all GameStates (e.g. game-configuration)
	 * @param width Width of the window
	 * @param height Height of the new window
	 * @param title Title of the new window
	 * @param antiAliasingLevel Level of antiAliasing to use or 0 to disable
	 */
	public GameContext() {
		try {
			settings = Settings.load(CONFIG_PATH);
			
		} catch (IOException e) {
			throw new Error("Unable to load config-file from '"+CONFIG_PATH.toAbsolutePath().toString()+"': "+e.getMessage(), e);
		}
		
		window = new RenderWindow();
		ContextSettings ctxSettings = new ContextSettings(settings.antiAliasingLevel);
		int style = settings.fullscreen ? WindowStyle.FULLSCREEN : WindowStyle.CLOSE|WindowStyle.TITLEBAR;
		
		window.create(new VideoMode(settings.width, settings.height), WINDOW_TITLE, style, ctxSettings);
		window.setVerticalSyncEnabled(settings.verticalSync);
		
		viewWidth = (int) ((VIEW_HEIGHT/window.getView().getSize().y) * window.getView().getSize().x);
		window.setView(new View(new Vector2f(viewWidth/2, VIEW_HEIGHT/2), new Vector2f(viewWidth, VIEW_HEIGHT)));
	}
	
	public Vector2f getMousePosition() {
		return window.mapPixelToCoords(Mouse.getPosition(window));
	}

	public int getViewHeight() {
		return VIEW_HEIGHT;
	}
	public int getViewWidth() {
		return viewWidth;
	}
	
}
