package de.secondsystem.game01.impl.intro;

import java.io.IOException;

import org.jsfml.graphics.ConstFont;
import org.jsfml.graphics.ConstTexture;
import org.jsfml.graphics.FloatRect;
import org.jsfml.graphics.RenderTarget;
import org.jsfml.graphics.Sprite;
import org.jsfml.graphics.Text;
import org.jsfml.window.Mouse;
import org.jsfml.window.Window;

import de.secondsystem.game01.impl.ResourceManager;
/*import org.jsfml.audio.Sound;
import org.jsfml.audio.SoundBuffer;*/
import org.jsfml.graphics.Color;
import org.jsfml.graphics.IntRect;

/**
 * This class provides a functional button with different attributes
 * @author Sebastian
 *
 */
public final class Button {

	String title, text;
	int pos_x, pos_y;
	IOnClickListener clickListener;
	
	final Text myText;
	final Sprite newsprite;
	final int height;
	final int width;
	
	// Sound buttonOver;
	
	
	// Constructors
	Button(String text, String file, String fonttype, int pos_x, int pos_y, IOnClickListener clickListener) {
		this.pos_x = pos_x;
		this.pos_y = pos_y;
		this.clickListener = clickListener;
		
		// Loading standard Font for buttons
		try {
		ConstFont myFont = ResourceManager.font.get(fonttype);
		
		// Loading Standard Texture for buttons
		ConstTexture newButton = ResourceManager.texture_gui.get(file);
		
		height = newButton.getSize().y / 3;
		width = newButton.getSize().x;
					
		// Button Sprite generation and positioning
		newsprite = new Sprite(newButton);
		newsprite.setPosition(pos_x, pos_y);
		changeTextureClip(0);
		
		// Button inner text generation, positioning and calibration
		myText = new Text(text, myFont, 26);
		FloatRect textRect = myText.getGlobalBounds();
		myText.setOrigin(textRect.width / 2, textRect.height / 1.5f);
		// TODO --> Alternative: myText.setOrigin(myText.getGlobalBounds().width / 2, myText.getGlobalBounds().height / 1.5f);
		myText.setPosition(newsprite.getPosition().x + width / 2, newsprite.getPosition().y + height / 2);

		/* TODO --> stabile sound implementations
		SoundBuffer buttonOverBuffer = new SoundBuffer();
		try {
		    buttonOverBuffer.loadFromFile(Paths.get("assets", "gui", "buttons", "Button_over.wav"));
		    System.out.println("Sound duration: " + buttonOverBuffer.getDuration().asSeconds() + " seconds");
		} catch(IOException ex) {
		    //Something went wrong
		    System.err.println("Failed to load the sound:");
		    ex.printStackTrace();
		}

		buttonOver = new Sound(buttonOverBuffer);
		
		SoundBuffer buttonPressedBuffer;*/
		
		} catch( IOException e ) {
			throw new Error(e.getMessage(), e);
		}
	}
	
	Button(String text, int pos_x, int pos_y, IOnClickListener clickListener) {
		this(text, "ButtonClass.png", "FreeSansBold.otf", pos_x, pos_y, clickListener);
	}
	
	Button(String text, int pos_x, int pos_y) {
		this(text, "ButtonClass.png", "FreeSansBold.otf", pos_x, pos_y, new IOnClickListener(){@Override public void onClick(){System.out.println("pressed");}});
	}
	
	
	// Interfaces	
	
	public interface IOnClickListener {
		void onClick();
	}
	
	
	// Methods
	// Draw a sprite
	void draw(RenderTarget rt){
		// TODO --> Decide if myText.setPosition should be done inside draw method
		// myText.setPosition(newsprite.getPosition().x + width / 2, newsprite.getPosition().y + height / 2);
		rt.draw(newsprite);
		rt.draw(myText);
	}
	
	void mouseover(Window window){
		if(this.newsprite.getGlobalBounds().contains(Mouse.getPosition(window).x, Mouse.getPosition(window).y)){
			changeTextureClip(Mouse.isButtonPressed(org.jsfml.window.Mouse.Button.LEFT) ? 2 : 1); myText.setColor(Color.RED);
			//System.out.println("  OVER  ");
			//buttonOver.play();
		} else {
			changeTextureClip(0); myText.setColor(Color.WHITE);
		}
	}
	
	private void changeTextureClip(int pos) {
		newsprite.setTextureRect(new IntRect(0,height*pos,width,height));
	}
	
}