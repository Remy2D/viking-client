package haven.purus;

import haven.Coord;
import haven.GOut;
import haven.Resource;
import haven.Tex;
import haven.UI;
import haven.Widget;

public class DoubleTapButton extends Widget {
    public static Tex button_on = Resource.loadtex("hud/fight_extension/dt_toggle_on");
    public static Tex button_off = Resource.loadtex("hud/fight_extension/dt_toggle_off");
    private final UI ui;
    private final static Integer POS_X = 950;
    private final static Integer POS_Y = 30;
    private final static Integer WIDTH = 32;
    private final static Integer HEIGHT = 50;

    public DoubleTapButton(UI ui) {
        super(ui, UI.scale(POS_X, POS_Y), UI.scale(WIDTH, HEIGHT));
        this.ui = ui;
    }

    public boolean mousedown(Coord c, int button) {
        if (button == 1) {
            ui.root.multiSessionWindow.toggleDoubleTapEnabled();
        }
        return (true);
    }

    @Override
    public void draw(GOut g) {
        if (ui.root.multiSessionWindow.doubleTapEnabled) {
            drawButton(g, button_on);
        } else {
            drawButton(g, button_off);
        }
    }

    private void drawButton(GOut g, Tex t) {
        if (t != null) {
            g.text("DT", new Coord((WIDTH - t.sz().x) / 2, 0));
            g.image(t, new Coord(0, 10), t.sz());
        }
    }
}
