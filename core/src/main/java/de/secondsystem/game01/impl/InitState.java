package de.secondsystem.game01.impl;

import de.secondsystem.game01.fsm.IContext;
import de.secondsystem.game01.fsm.IState;

/**
 * Erster Status nach dem Starten der Anwendung
 * Initialisiert den GameContext
 * @author lowkey
 *
 */
public class InitState implements IState {

	private final IState nextState;
	private GameContext ctx;
	
	/**
	 * @param nextState Zustand nach der Initialisierung
	 */
	public InitState(IState nextState) {
		this.nextState = nextState;
	}

	@Override
	public IState update() {
		ctx = new GameContext();
		
		return nextState;
	}

	@Override
	public void enter(IContext ctx) {
	}

	@Override
	public IContext exit() {
		ctx.window.clear();
		return ctx;
	}

}
