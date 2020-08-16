package haven.purus;

import haven.*;
import haven.Button;
import haven.Label;
import haven.Window;

import java.awt.event.KeyEvent;
import java.util.List;

public class KeyBindingWnd extends Window {

	// Next key-event shall change this button
	private KeyBindings.KeyBinding changeKey;
	// Next key-event shall update this label
	private Label changeKeyLabel;

	public KeyBindingWnd(List<KeyBindings.KeyBinding> kbList) {
		super(new Coord(400, 330), "Keybindings");

		add(new Label("Key"), new Coord(190, 5));
		add(new Label("Ctrl"), new Coord(220, 5));
		add(new Label("Shift"), new Coord(250, 5));
		add(new Label("Alt"), new Coord(289, 5));

		int y = 25;
		for(KeyBindings.KeyBinding kb:kbList) {
			add(new Label(kb.getDesc()), new Coord(0, y+5));

			Label keyLabel = add(new Label(KeyEvent.getKeyText(kb.getCurKeyCode())), new Coord(190, y+5));

			CheckBox ctrlCkb = add(new CheckBox("") {
				{
					a = kb.isCurCtrlMod();
				}

				@Override
				public void set(boolean val) {
					if(val) {
						kb.setCurCtrlMod(true);
						a = true;
					} else {
						kb.setCurCtrlMod(false);
						a = false;
					}
				}
			}, new Coord(225, y+5));

			add(new Button(20, "Change") {
				@Override
				public void click() {
					changeKey = kb;
					changeKeyLabel = keyLabel;
				}
			}, new Coord(110, y));

			CheckBox shiftCkb = add(new CheckBox("") {
				{
					a = kb.isCurShiftMod();
				}

				@Override
				public void set(boolean val) {
					if(val) {
						kb.setCurShiftMod(true);
						a = true;
					} else {
						kb.setCurShiftMod(false);
						a = false;
					}
				}
			}, new Coord(257, y+5));

			CheckBox altCkb = add(new CheckBox("") {
				{
					a = kb.isCurAltMod();
				}

				@Override
				public void set(boolean val) {
					if(val) {
						kb.setCurAltMod(true);
						a = true;
					} else {
						kb.setCurAltMod(false);
						a = false;
					}
				}

			}, new Coord(289, y+5));

			add(new Button(20, "Reset") {
				@Override
				public void click() {
					kb.resetDefaults();
					keyLabel.settext(KeyEvent.getKeyText(kb.getCurKeyCode()));
					ctrlCkb.a = kb.isCurCtrlMod();
					shiftCkb.a = kb.isCurShiftMod();
					altCkb.a = kb.isCurAltMod();
				}
			}, new Coord(325, y));

			y += 25;
		}
	}

	public boolean keydown(KeyEvent e) {
		if(changeKey != null) {
			changeKey.setCurKeyCode(e.getKeyCode());
			changeKeyLabel.settext(KeyEvent.getKeyText(e.getKeyCode()));
			changeKey = null;
			changeKeyLabel = null;
			return true;
		}
		return false;
	}

	public void wdgmsg(Widget sender, String msg, Object... args) {
		if (sender == cbtn) {
			gameui().keyBindingWnd.hide();
		} else {
			super.wdgmsg(sender, msg, args);
		}
	}
}
