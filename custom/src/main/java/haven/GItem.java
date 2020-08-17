/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import haven.res.ui.tt.q.qbuff.QBuff;
import integrations.food.FoodService;
import integrations.food.IconService;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static haven.Text.num10Fnd;

public class GItem extends AWidget implements ItemInfo.SpriteOwner, GSprite.Owner {
    public Indir<Resource> res;
    public MessageBuf sdt;
    public int meter = 0;
    public int num = -1;
    private GSprite spr;
    private ItemInfo.Raw rawinfo;
    public List<ItemInfo> info = Collections.emptyList();
    public QBuff quality;
    public Tex metertex;
    public double studytime = 0.0;
    public boolean drop = false;
    private double dropTimer = 0;
    private boolean postProcessed = false;

    @RName("item")
    public static class $_ implements Factory {
        public Widget create(UI ui, Object[] args) {
            int res = (Integer) args[0];
            Message sdt = (args.length > 1) ? new MessageBuf((byte[]) args[1]) : Message.nil;
            return (new GItem(ui.sess.getres(res), sdt));
        }
    }

    public interface ColorInfo {
        public Color olcol();
    }

    public interface OverlayInfo<T> {
        public T overlay();
        public void drawoverlay(GOut g, T data);
    }

    public static class InfoOverlay<T> {
        public final OverlayInfo<T> inf;
        public final T data;

        public InfoOverlay(OverlayInfo<T> inf) {
            this.inf = inf;
            this.data = inf.overlay();
        }

        public void draw(GOut g) {
            inf.drawoverlay(g, data);
        }

        public static <S> InfoOverlay<S> create(OverlayInfo<S> inf) {
            return(new InfoOverlay<S>(inf));
        }
    }

    public interface NumberInfo extends OverlayInfo<Tex> {
        public int itemnum();
        public default Color numcolor() {
            return(Color.WHITE);
        }

        public default Tex overlay() {
            return(new TexI(GItem.NumberInfo.numrender(itemnum(), numcolor())));
        }

        public default void drawoverlay(GOut g, Tex tex) {
            g.aimage(tex, new Coord(g.sz.x, 0), 1, 0);
        }

        public static BufferedImage numrender(int num, Color col) {
            return Text.renderstroked(num + "", col, Color.BLACK).img;
        }
    }

    public interface MeterInfo {
        public double meter();
    }


    public static class Amount extends ItemInfo implements NumberInfo {
        private final int num;

        public Amount(Owner owner, int num) {
            super(owner);
            this.num = num;
        }

        public int itemnum() {
            return (num);
        }
    }

    public GItem(Indir<Resource> res, Message sdt) {
        this.res = res;
        this.sdt = new MessageBuf(sdt);
    }

    public GItem(Indir<Resource> res) {
        this(res, Message.nil);
    }

    public String getname() {
        if (rawinfo == null) {
            return "";
        }

        try {
            return ItemInfo.find(ItemInfo.Name.class, info()).str.text;
        } catch (Exception ex) {
            return "";
        }
    }

    private Random rnd = null;

    public Random mkrandoom() {
        if (rnd == null)
            rnd = new Random();
        return (rnd);
    }

    public Resource getres() {
        return (res.get());
    }

    private static final OwnerContext.ClassResolver<GItem> ctxr = new OwnerContext.ClassResolver<GItem>()
            .add(Glob.class, wdg -> wdg.ui.sess.glob)
            .add(Session.class, wdg -> wdg.ui.sess);

    public <T> T context(Class<T> cl) {
        return (ctxr.context(cl, this));
    }

    @Deprecated
    public Glob glob() {
        return (ui.sess.glob);
    }

    public GSprite spr() {
        GSprite spr = this.spr;
        if (spr == null) {
            try {
                spr = this.spr = GSprite.create(this, res.get(), sdt.clone());
                if (!postProcessed) {
                    dropItMaybe();
                    postProcessed = true;
                }
                IconService.checkIcon(info(), spr);
            } catch (Loading l) {
            }
        }
        return (spr);
    }

    public void tick(double dt) {
    	super.tick(dt);
        if(drop) {
        	dropTimer += dt;
        	if(dropTimer > 0.1) {
        		dropTimer = 0;
        		wdgmsg("drop", Coord.z);
        	}
        }
        GSprite spr = spr();
        if (spr != null)
            spr.tick(dt);
    }

    public List<ItemInfo> info() {
        if (info == null && rawinfo != null) {
            info = ItemInfo.buildinfo(this, rawinfo);
			info.add(new ItemInfo.AdHoc(this, (Config.resinfo ? ("\n" + this.getres().name) : "")));
			try {
                // getres() can throw Loading, ignore it
                FoodService.checkFood(info, getres().name);
            } catch (Exception ex) {
			}
        }
        return (info);
    }

    public Resource resource() {
        return (res.get());
    }

    public GSprite sprite() {
        if (spr == null)
            throw (new Loading("Still waiting for sprite to be constructed"));
        return (spr);
    }

    public void uimsg(String name, Object... args) {
        if (name == "num") {
            num = (Integer) args[0];
        } else if (name == "chres") {
            synchronized (this) {
                res = ui.sess.getres((Integer) args[0]);
                sdt = (args.length > 1) ? new MessageBuf((byte[]) args[1]) : MessageBuf.nil;
                spr = null;
            }
        } else if (name == "tt") {
            info = null;
            if (rawinfo != null)
                quality = null;
            rawinfo = new ItemInfo.Raw(args);
        } else if (name == "meter") {
            meter = (int)((Number)args[0]).doubleValue();
            metertex = Text.renderstroked(String.format("%d%%", meter), Color.WHITE, Color.BLACK, num10Fnd).tex();
        }
    }

    public void qualitycalc(List<ItemInfo> infolist) {
        for (ItemInfo info : infolist) {
            if (info instanceof QBuff) {
                this.quality = (QBuff) info;
                break;
            }
        }
    }

    public QBuff quality() {
        if (quality == null) {
            try {
                for (ItemInfo info : info()) {
                    if (info instanceof ItemInfo.Contents) {
                        qualitycalc(((ItemInfo.Contents) info).sub);
                        return quality;
                    }
                }
                qualitycalc(info());
            } catch (Loading l) {
            }
        }
        return quality;
    }

    public ItemInfo.Contents getcontents() {
        try {
            for (ItemInfo info : info()) {
                if (info instanceof ItemInfo.Contents)
                    return (ItemInfo.Contents) info;
            }
        } catch (Exception e) { // fail silently if info is not ready
        }
        return null;
    }

    private void dropItMaybe() {
        Resource curs = ui.root.getcurs(Coord.z);
        if (Config.dropEverything) {
            this.wdgmsg("drop", Coord.z);
        } else {
            String name = this.resource().basename();
            if ((Config.dropSoil && name.equals("soil")))
                this.wdgmsg("drop", Coord.z);
            if (curs != null && curs.name.equals("gfx/hud/curs/mine") &&
                    (Config.dropMinedStones && Config.mineablesStone.contains(name) ||
                    Config.dropMinedOre && Config.mineablesOre.contains(name) ||
                    Config.dropMinedOrePrecious && Config.mineablesOrePrecious.contains(name) ||
                    Config.dropMinedCurios && Config.mineablesCurios.contains(name)))
                this.wdgmsg("drop", Coord.z);
        }
    }

    public Coord size() {
    	try {
			Indir<Resource> res = getres().indir();
			if(res.get() != null && res.get().layer(Resource.imgc) != null) {
				Tex tex = res.get().layer(Resource.imgc).tex();
				if(tex == null)
					return new Coord(1, 1);
				else
					return tex.sz().div(30);
			} else {
				return new Coord(1, 1);
			}
		} catch(Loading l) {

		}
    	return new Coord(1, 1);
    }

}
