package de.secondsystem.game01.impl.graphic;

import java.io.IOException;
import java.nio.file.Paths;

import org.jsfml.graphics.IntRect;
import org.jsfml.graphics.RenderTarget;
import org.jsfml.graphics.Sprite;
import org.jsfml.graphics.Texture;
import org.jsfml.system.Vector2f;


// temporary solution, end solution depends on the animated sprites
public class AnimatedSprite {
	
	private Sprite  sprite = new Sprite();
	private int frameWidth;
	private int frameHeight;
	private int numFramesX;

	public AnimatedSprite(String textureFilename, float x, float y, int numFrames,
            int frameWidth, int frameHeight)
	{
		// create texture
		Texture tex = new Texture();
		
		try {
			// TODO: load textures from a file
			tex.loadFromFile(Paths.get("assets/sprites/"+textureFilename));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		sprite.setTexture(tex);
		sprite.setPosition(new Vector2f(x,y));
		sprite.setOrigin(frameWidth/2f, frameHeight/2f);
		
		numFramesX = (int) Math.round((double)tex.getSize().x / frameWidth);
		this.frameWidth  = frameWidth;
		this.frameHeight = frameHeight;
		
	}
	
    public void draw(RenderTarget rt, float frameNum)
    {
    	int column = (int) frameNum % numFramesX;
    	
    	int row = (int) frameNum / numFramesX;
    	
    	IntRect rect = new IntRect(column * frameWidth, row * frameHeight, frameWidth, frameHeight);
    	
    	sprite.setTextureRect(rect);
    	rt.draw(sprite);
	}

	
	public Sprite getSprite()
	{
		return sprite;
	}
	
	public int getFrameWidth()
	{
		return frameWidth;
	}
	
	public int getFrameHeight()
	{
		return frameHeight;
	}
	
	
}
