package haven.sloth.gui;

import haven.Coord;
import haven.Label;
import haven.Widget;
import haven.Window;

public class DowseWnd extends Window {
	private final Runnable onClose;

	public DowseWnd(final double a1, final double a2,
					final Runnable onClose) {
		super(Coord.z, "Dowse");
		this.onClose = onClose;
		final Label la1 = new Label(String.format("Left Angle: %.2f", Math.toDegrees(a1)));
		final Label la2 = new Label(String.format(" Right Angle: %.2f", Math.toDegrees(a2)));
		final int spacer = 5;
		add(la1, Coord.z);
		add(la2, new Coord(0, la1.sz.y + spacer));
		pack();
	}

	public void wdgmsg(Widget sender, String msg, Object... args) {
		if (sender == cbtn) {
			onClose.run();
			ui.destroy(this);
		}
		super.wdgmsg(sender, msg, args);
	}
}
