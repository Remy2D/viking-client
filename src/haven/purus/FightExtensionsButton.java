package haven.purus;

import haven.Coord;
import haven.GOut;
import haven.Resource;
import haven.Tex;
import haven.UI;
import haven.Widget;

public class FightExtensionsButton extends Widget {
    public static Tex button_on = Resource.loadtex("hud/fight_extension/fe_toggle_on");
    public static Tex button_off = Resource.loadtex("hud/fight_extension/fe_toggle_off");
    private final UI ui;
    private final static Integer POS_X = 900;
    private final static Integer POS_Y = 30;
    private final static Integer WIDTH = 32;
    private final static Integer HEIGHT = 40;

    public FightExtensionsButton(UI ui) {
        super(ui, UI.scale(POS_X, POS_Y), UI.scale(WIDTH, HEIGHT));
        this.ui = ui;
    }

    public boolean mousedown(Coord c, int button) {
        if (button == 1) {
            ui.root.multiSessionWindow.toggleFightExtensionsEnabled();
        }
        return (true);
    }

    @Override
    public void draw(GOut g) {
        if (ui.root.multiSessionWindow.fightExtensionsEnabled) {
            drawButton(g, button_on);
        } else {
            drawButton(g, button_off);
        }
    }

    private void drawButton(GOut g, Tex t) {
        if (t != null) {
            g.text("FExt", new Coord((WIDTH - t.sz().x) / 2, 0));
            g.image(t, new Coord(0, 10), t.sz());
        }
    }
}
