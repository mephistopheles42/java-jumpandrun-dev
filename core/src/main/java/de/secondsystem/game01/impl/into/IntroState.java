package de.secondsystem.game01.impl.into;

import org.jsfml.window.event.Event;

import de.secondsystem.game01.impl.GameContext;
import de.secondsystem.game01.impl.GameState;

/**
 * Zustand für Intro (erster Zustand nach Initialisierung)
 * Beispiel für weitere Zustände
 * @author lowkey
 *
 */
public final class IntroState extends GameState {

	@Override
	protected void onStart(GameContext ctx) {
		// TODO
	}

	@Override
	protected void onStop(GameContext ctx) {
		// TODO
	}

	@Override
	protected void onFrame(GameContext ctx) {
		// TODO
		for(Event event : ctx.window.pollEvents()) {
	        if(event.type == Event.Type.CLOSED) {
	            //The user pressed the close button
	            ctx.window.close();
	        }
	    }
	}

}