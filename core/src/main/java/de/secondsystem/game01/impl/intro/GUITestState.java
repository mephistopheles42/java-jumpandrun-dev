/**
 * 
 */
package de.secondsystem.game01.impl.intro;

import java.io.IOException;

import org.jsfml.graphics.ConstFont;
import org.jsfml.graphics.Sprite;
import org.jsfml.graphics.Text;
import org.jsfml.graphics.Texture;
import org.jsfml.graphics.TextureCreationException;
import org.jsfml.window.Mouse;
import org.jsfml.window.Keyboard.Key;
import org.jsfml.window.event.Event;

import de.secondsystem.game01.impl.GameContext;
import de.secondsystem.game01.impl.GameState;
import de.secondsystem.game01.impl.ResourceManager;

/**
 * @author Sebastian
 *
 */
public final class GUITestState extends GameState {

	private final GameState playGameState, MainMenu;
	private Sprite backdrop = new Sprite();
	
	private final Text infoInputText;
	
	InputText testtext = new InputText(50, 50, 200, "");
	
	MenuButton testButton = new MenuButton("TEST BUTTON", 50, 100, new MenuButton.IOnClickListener(){
		
		@Override
		public void onClick() {
			System.out.println("Test Button works!");
		}
	});

		
	public GUITestState(GameState MainMenu, GameState playGameState,
			Sprite backdrop) {
		// Transfering last State into playGameState
		this.playGameState = playGameState;
		this.MainMenu = MainMenu;
		this.backdrop = backdrop;
		try {
			// Loading standard Font
			ConstFont myFont = ResourceManager.font.get("VeraMono.ttf");
			infoInputText = new Text("Input Text", myFont, 20);
			} catch( IOException e ) {
				throw new Error(e.getMessage(), e);
			}
	}
	
	
	@Override
	protected void onStart(GameContext ctx) {

		if (backdrop.getTexture() == null) {
			Texture backdropBuffer = new Texture();
			// Creating Backdrop Texture via monitor screenshot of the stage
			// before, rendered on every frame
			try {
				backdropBuffer.create(ctx.settings.width, ctx.settings.height);
			} catch (TextureCreationException e) {
				e.printStackTrace();
			}
			backdropBuffer.update(ctx.window);
			backdrop.setTexture(backdropBuffer);
		}
		
		infoInputText.setPosition(testtext.pos_x, testtext.pos_y - 25);
	}

	@Override
	protected void onStop(GameContext ctx) {
		// TODO
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	protected void onFrame(GameContext ctx, long frameTime) {
		
		ctx.window.draw(backdrop);
		
		for (Event event : ctx.window.pollEvents()) {
			switch (event.type) {
			case CLOSED:
				ctx.window.close();
				break;
			case MOUSE_BUTTON_RELEASED:
				if( event.asMouseButtonEvent().button == org.jsfml.window.Mouse.Button.LEFT ) {
					testButton.onButtonReleased(event.asMouseButtonEvent().position.x, event.asMouseButtonEvent().position.y);
					if(Mouse.getPosition(ctx.window).x >= testtext.pos_x && Mouse.getPosition(ctx.window).x <= testtext.pos_x + testtext.width  && 
							   Mouse.getPosition(ctx.window).y >= testtext.pos_y && Mouse.getPosition(ctx.window).y <= testtext.pos_y + testtext.height){
								testtext.setActive();
					} else { testtext.setInactive();}					
				}
				break;
			case TEXT_ENTERED:
				if(event.asTextEvent().unicode <= 127 && event.asTextEvent().unicode >= 32){
					//System.out.println("TEXT ENTERED UNICODE: " + event.asTextEvent().unicode);
					testtext.newKey(event);
				// Backspace pushed
				} else if (event.asTextEvent().unicode == 8){
					testtext.removeKey();
				// Return pushed
				} else if (event.asTextEvent().unicode == 13){
					System.out.println("Sent Text: " + testtext.finalizeInput());
				}
				break;
			case KEY_RELEASED:
				if (event.asKeyEvent().key == Key.ESCAPE)
					testtext.setInactive();
				if ( playGameState!=null && event.asKeyEvent().key == Key.ESCAPE)
					setNextState(playGameState);
			}
		}

		ctx.window.draw(infoInputText);
		testtext.draw(ctx.window);
		testButton.draw(ctx.window);
		
	}	
	
	
	
	
	
}