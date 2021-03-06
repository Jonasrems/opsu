/*
 * opsu! - an open-source osu! client
 * Copyright (C) 2014, 2015 Jeffrey Han
 *
 * opsu! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!.  If not, see <http://www.gnu.org/licenses/>.
 */

package itdelatrisu.opsu.states;

import itdelatrisu.opsu.GameImage;
import itdelatrisu.opsu.Opsu;
import itdelatrisu.opsu.Options;
import itdelatrisu.opsu.Utils;
import itdelatrisu.opsu.audio.MusicController;
import itdelatrisu.opsu.audio.SoundController;
import itdelatrisu.opsu.audio.SoundEffect;
import itdelatrisu.opsu.ui.MenuButton;
import itdelatrisu.opsu.ui.UI;

import org.lwjgl.input.Keyboard;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;
import org.newdawn.slick.state.transition.FadeInTransition;
import org.newdawn.slick.state.transition.FadeOutTransition;

/**
 * "Game Pause/Fail" state.
 * <ul>
 * <li>[Continue] - unpause game (return to game state)
 * <li>[Retry]    - restart game (return to game state)
 * <li>[Back]     - return to song menu state
 * </ul>
 */
public class GamePauseMenu extends BasicGameState {
	/** Music fade-out time, in milliseconds. */
	private static final int FADEOUT_TIME = 1000;

	/** Track position when the pause menu was loaded (for FADEOUT_TIME). */
	private long pauseStartTime;

	/** "Continue", "Retry", and "Back" buttons. */
	private MenuButton continueButton, retryButton, backButton;

	// game-related variables
	private GameContainer container;
	private StateBasedGame game;
	private Input input;
	private int state;
	private Game gameState;

	public GamePauseMenu(int state) {
		this.state = state;
	}

	@Override
	public void init(GameContainer container, StateBasedGame game)
			throws SlickException {
		this.container = container;
		this.game = game;
		this.input = container.getInput();
		this.gameState = (Game) game.getState(Opsu.STATE_GAME);
	}

	@Override
	public void render(GameContainer container, StateBasedGame game, Graphics g)
			throws SlickException {
		// get background image
		GameImage bg = (gameState.getRestart() == Game.Restart.LOSE) ?
				GameImage.FAIL_BACKGROUND : GameImage.PAUSE_OVERLAY;

		// don't draw default background if button skinned and background unskinned
		boolean buttonsSkinned =
			GameImage.PAUSE_CONTINUE.hasSkinImage() ||
			GameImage.PAUSE_RETRY.hasSkinImage() ||
			GameImage.PAUSE_BACK.hasSkinImage();
		if (!buttonsSkinned || bg.hasSkinImage())
			bg.getImage().draw();
		else
			g.setBackground(Color.black);

		// draw buttons
		if (gameState.getRestart() != Game.Restart.LOSE)
			continueButton.draw();
		retryButton.draw();
		backButton.draw();

		UI.draw(g);
	}

	@Override
	public void update(GameContainer container, StateBasedGame game, int delta)
			throws SlickException {
		UI.update(delta);
		int mouseX = input.getMouseX(), mouseY = input.getMouseY();
		continueButton.hoverUpdate(delta, mouseX, mouseY);
		retryButton.hoverUpdate(delta, mouseX, mouseY);
		backButton.hoverUpdate(delta, mouseX, mouseY);
	}

	@Override
	public int getID() { return state; }

	@Override
	public void keyPressed(int key, char c) {
		// game keys
		if (!Keyboard.isRepeatEvent()) {
			if (key == Options.getGameKeyLeft())
				mousePressed(Input.MOUSE_LEFT_BUTTON, input.getMouseX(), input.getMouseY());
			else if (key == Options.getGameKeyRight())
				mousePressed(Input.MOUSE_RIGHT_BUTTON, input.getMouseX(), input.getMouseY());
		}

		switch (key) {
		case Input.KEY_ESCAPE:
			// 'esc' will normally unpause, but will return to song menu if health is zero
			if (gameState.getRestart() == Game.Restart.LOSE) {
				SoundController.playSound(SoundEffect.MENUBACK);
				((SongMenu) game.getState(Opsu.STATE_SONGMENU)).resetGameDataOnLoad();
				MusicController.playAt(MusicController.getBeatmap().previewTime, true);
				if (UI.getCursor().isSkinned())
					UI.getCursor().reset();
				game.enterState(Opsu.STATE_SONGMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
			} else {
				SoundController.playSound(SoundEffect.MENUBACK);
				gameState.setRestart(Game.Restart.FALSE);
				game.enterState(Opsu.STATE_GAME);
			}
			break;
		case Input.KEY_R:
			// restart
			if (input.isKeyDown(Input.KEY_RCONTROL) || input.isKeyDown(Input.KEY_LCONTROL)) {
				gameState.setRestart(Game.Restart.MANUAL);
				game.enterState(Opsu.STATE_GAME);
			}
			break;
		case Input.KEY_F7:
			Options.setNextFPS(container);
			break;
		case Input.KEY_F10:
			Options.toggleMouseDisabled();
			break;
		case Input.KEY_F12:
			Utils.takeScreenShot();
			break;
		}
	}

	@Override
	public void mousePressed(int button, int x, int y) {
		if (button == Input.MOUSE_MIDDLE_BUTTON)
			return;

		boolean loseState = (gameState.getRestart() == Game.Restart.LOSE);

		// if music faded out (i.e. health is zero), don't process any actions before FADEOUT_TIME
		if (loseState && System.currentTimeMillis() - pauseStartTime < FADEOUT_TIME)
			return;

		if (continueButton.contains(x, y) && !loseState) {
			SoundController.playSound(SoundEffect.MENUBACK);
			gameState.setRestart(Game.Restart.FALSE);
			game.enterState(Opsu.STATE_GAME);
		} else if (retryButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUHIT);
			gameState.setRestart(Game.Restart.MANUAL);
			game.enterState(Opsu.STATE_GAME);
		} else if (backButton.contains(x, y)) {
			SoundController.playSound(SoundEffect.MENUBACK);
			((SongMenu) game.getState(Opsu.STATE_SONGMENU)).resetGameDataOnLoad();
			if (loseState)
				MusicController.playAt(MusicController.getBeatmap().previewTime, true);
			else
				MusicController.resume();
			if (UI.getCursor().isSkinned())
				UI.getCursor().reset();
			game.enterState(Opsu.STATE_SONGMENU, new FadeOutTransition(Color.black), new FadeInTransition(Color.black));
		}
	}

	@Override
	public void mouseWheelMoved(int newValue) {
		if (Options.isMouseWheelDisabled() || Options.isMouseDisabled())
			return;

		UI.changeVolume((newValue < 0) ? -1 : 1);
	}

	@Override
	public void enter(GameContainer container, StateBasedGame game)
			throws SlickException {
		UI.enter();
		pauseStartTime = System.currentTimeMillis();
		if (gameState.getRestart() == Game.Restart.LOSE) {
			MusicController.fadeOut(FADEOUT_TIME);
			SoundController.playSound(SoundEffect.FAIL);
		} else
			MusicController.pause();
		continueButton.resetHover();
		retryButton.resetHover();
		backButton.resetHover();
	}

	/**
	 * Loads all game pause/fail menu images.
	 */
	public void loadImages() {
		int width = container.getWidth();
		int height = container.getHeight();

		// initialize buttons
		continueButton = new MenuButton(GameImage.PAUSE_CONTINUE.getImage(), width / 2f, height * 0.25f);
		retryButton = new MenuButton(GameImage.PAUSE_RETRY.getImage(), width / 2f, height * 0.5f);
		backButton = new MenuButton(GameImage.PAUSE_BACK.getImage(), width / 2f, height * 0.75f);
		continueButton.setHoverExpand();
		retryButton.setHoverExpand();
		backButton.setHoverExpand();
	}
}
