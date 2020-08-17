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

import haven.purus.Iconfinder;
import haven.purus.alarms.AlarmManager;
import haven.purus.mapper.Mapper;
import haven.resutil.Ridges;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;
import static haven.OCache.posres;

public class LocalMiniMap extends Widget {
    private static final Tex resize = Resource.loadtex("gfx/hud/wndmap/lg/resize");
    private static final Tex gridblue = Resource.loadtex("gfx/hud/mmap/gridblue");
    private static final Tex gridred = Resource.loadtex("gfx/hud/mmap/gridred");
    public final MapView mv;
    public MapFile save;
    private Coord cc = null;
    public MapTile cur = null;
    private UI.Grab dragging;
    private Coord doff = Coord.z;
    private Coord delta = Coord.z;
    private static final Resource alarmplayerWhite = Resource.local().loadwait("sfx/alarms/whitePlayer");
    private static final Resource alarmplayerRed = Resource.local().loadwait("sfx/alarms/redPlayer");
	/*private static final Resource alarmplayersfx = Resource.local().loadwait("sfx/alarmplayer");
    private static final Resource foragablesfx = Resource.local().loadwait("sfx/awwyeah");
    private static final Resource voiveljet = Resource.local().loadwait("sfx/alarms/voiveljet");
    private static final Resource wolf1 = Resource.local().loadwait("sfx/alarms/wolf1");
    private static final Resource wolf2 = Resource.local().loadwait("sfx/alarms/wolf2");
    private static final Resource wolf3 = Resource.local().loadwait("sfx/alarms/wolf3");
    private static final Resource bearsfx = Resource.local().loadwait("sfx/alarms/bear");
    private static final Resource lynxfx = Resource.local().loadwait("sfx/lynx");
    private static final Resource walrusfx = Resource.local().loadwait("sfx/alarms/walrus");
    private static final Resource sealsfx = Resource.local().loadwait("sfx/seal");
    private static final Resource trollsfx = Resource.local().loadwait("sfx/troll");
    private static final Resource mammothsfx = Resource.local().loadwait("sfx/mammoth");
    private static final Resource doomedsfx = Resource.local().loadwait("sfx/alarms/bram");
    private static final Resource eaglesfx = Resource.local().loadwait("sfx/eagle");
    private static final Resource swagsfx = Resource.local().loadwait("sfx/swag");
    private static final Resource wolfsfx = Resource.local().loadwait("sfx/wolf");*/
	private final HashSet<Long> sgobs = new HashSet<Long>();
	private final TreeSet<Long> sentMapGobs = new TreeSet<Long>();

	TexI loadedGrid = null;
	
	private float zoom = 1f; //zoom multiplier
	private float iconZoom = 1f; //zoom multiplier for minimap icons
	
    private final Map<Coord, Tex> maptiles = new LinkedHashMap<Coord, Tex>(100, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Coord, Tex> eldest) {
            if (size() > 100) {
                try {
                    eldest.getValue().dispose();
                } catch (RuntimeException e) {
                }
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            values().forEach(Tex::dispose);
            super.clear();
        }
    };
    private final Map<Pair<MCache.Grid, Integer>, Defer.Future<MapTile>> cache = new LinkedHashMap<Pair<MCache.Grid, Integer>, Defer.Future<MapTile>>(7, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<Pair<MCache.Grid, Integer>, Defer.Future<MapTile>> eldest) {
            return size() > 7;
        }
    };
    private final static Tex bushicn = Text.renderstroked("\u22C6", Color.CYAN, Color.BLACK, Text.num12boldFnd).tex();
    private final static Tex treeicn = Text.renderstroked("\u25B2", Color.CYAN, Color.BLACK, Text.num12boldFnd).tex();
    private final static Tex bldricn = Text.renderstroked("\u25AA", Color.CYAN, Color.BLACK, Text.num12boldFnd).tex();
    private Map<Color, Tex> xmap = new HashMap<Color, Tex>(6);
    public static Coord plcrel = null;
    public long lastnewgid;


    public static class MapTile {
        public MCache.Grid grid;
        public int seq;

        public MapTile(MCache.Grid grid, int seq) {
            this.grid = grid;
            this.seq = seq;
        }
    }

    private BufferedImage tileimg(int t, BufferedImage[] texes) {
        BufferedImage img = texes[t];
        if (img == null) {
            Resource r = ui.sess.glob.map.tilesetr(t);
            if (r == null)
                return (null);
            Resource.Image ir = r.layer(Resource.imgc);
            if (ir == null)
                return (null);
            img = ir.img;
            texes[t] = img;
        }
        return (img);
    }

    public Tex drawmap(Coord ul, BufferedImage[] texes) {
        Coord sz = cmaps;
        MCache m = ui.sess.glob.map;
        BufferedImage buf = TexI.mkbuf(sz);
        Coord c = new Coord();
        for (c.y = 0; c.y < sz.y; c.y++) {
            for (c.x = 0; c.x < sz.x; c.x++) {
                int t = Config.minimapsmooth ? m.gettilenosnow(ul.add(c)) : m.gettile(ul.add(c));
                BufferedImage tex = tileimg(t, texes);
                int rgb = 0;
                if (tex != null)
                    rgb = tex.getRGB(Utils.floormod(c.x + ul.x, tex.getWidth()),
                            Utils.floormod(c.y + ul.y, tex.getHeight()));
                buf.setRGB(c.x, c.y, rgb);
                if (!Config.minimapsmooth) {
                    try {
                        if ((m.gettile(ul.add(c).add(-1, 0)) > t) ||
                                (m.gettile(ul.add(c).add(1, 0)) > t) ||
                                (m.gettile(ul.add(c).add(0, -1)) > t) ||
                                (m.gettile(ul.add(c).add(0, 1)) > t)) {
                            buf.setRGB(c.x, c.y, Color.BLACK.getRGB());
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }

        for (c.y = 1; c.y < sz.y - 1; c.y++) {
            for (c.x = 1; c.x < sz.x - 1; c.x++) {
                try {
                    int t = m.gettile(ul.add(c));
                    Tiler tl = m.tiler(t);
                    if (tl instanceof Ridges.RidgeTile) {
                        if (Ridges.brokenp(m, ul.add(c))) {
                            for (int y = c.y - 1; y <= c.y + 1; y++) {
                                for (int x = c.x - 1; x <= c.x + 1; x++) {
                                    Color cc = new Color(buf.getRGB(x, y));
                                    buf.setRGB(x, y, Utils.blendcol(cc, Color.BLACK, ((x == c.x) && (y == c.y)) ? 1 : 0.1).getRGB());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }
        }

        return new TexI(buf);
    }

    public LocalMiniMap(Coord sz, MapView mv) {
        super(sz);
        this.mv = mv;
        // Initialize gob visibility box
		BufferedImage bi = new BufferedImage(84, 84, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gr = bi.createGraphics();
		gr.setColor(new Color(0, 103, 255, 42));
		gr.fillRect(0, 0, bi.getWidth()-1, bi.getHeight()-1);
		gr.setColor(Color.BLUE);
		gr.drawRect(0, 0, bi.getWidth()-1, bi.getHeight()-1);
		gr.drawImage(bi, null, 0, 0);
		loadedGrid = new TexI(bi);
    }

    public void save(MapFile file) {
        this.save = file;
    }

    public Coord p2c(Coord2d pc) {
    	return (pc.floor(tilesz).sub(cc).mul(zoom).add(sz.div(2)));
    }

    public Coord2d c2p(Coord c) {
        return (c.sub(sz.div(2)).div(zoom).add(cc).mul(tilesz).add(tilesz.div(2)));
    }

    public void drawicons(GOut g) {
        OCache oc = ui.sess.glob.oc;
        List<Gob> dangergobs = new ArrayList<Gob>();
        synchronized (oc) {
            Gob player = mv.player();
            for (Gob gob : oc) {
                try {
                    Resource res = gob.getres();
                    if (res == null)
                        continue;

                    String basename = res.basename();
                    GobIcon icon = gob.getattr(GobIcon.class);
                    if (icon != null || Config.additonalicons.containsKey(res.name)) {
                        if (Gob.Type.MOB.has(gob.type) || gob.type == Gob.Type.BAT) {
                            dangergobs.add(0, gob);
                        } else {
                            CheckListboxItem itm = Config.icons.get(basename);
                            if (itm == null || !itm.selected) {
                                Tex tex;
                                if (icon != null)
                                    tex = gob.knocked == Boolean.TRUE ? icon.texgrey() : icon.tex();
                                else
                                    tex = Config.additonalicons.get(res.name);
                                g.image(tex, p2c(gob.rc).sub(tex.sz().mul(iconZoom).div(2)).add(delta), tex.dim.mul(iconZoom));
                            }
                        }
                    } else if (gob.type == Gob.Type.PLAYER && player != null && gob.id != player.id) {
                        dangergobs.add(gob);
                        continue;
                    }

                    if (gob.type == Gob.Type.BOULDER) {
                        CheckListboxItem itm = Config.boulders.get(basename.substring(0, basename.length() - 1));
                        if (itm != null && itm.selected) {
                            if(Iconfinder.icons.containsKey(basename.substring(0, basename.length() - 1))) {
                                try {
                                    Tex tex = Resource.remote().loadwait(Iconfinder.icons.get(basename.substring(0, basename.length() - 1))).layer(Resource.imgc).tex();
                                    g.image(tex, p2c(gob.rc).sub(tex.sz().mul(iconZoom).div(4)).add(delta), tex.dim.mul(iconZoom/2));
                                } catch(Resource.Loading l) {

                                }
                            } else {
                                g.image(bldricn, p2c(gob.rc).add(delta).sub(bldricn.sz().div(2)));
                            }
                        }
                    } else if (gob.type == Gob.Type.BUSH) {
                        CheckListboxItem itm = Config.bushes.get(basename);
                        if (itm != null && itm.selected) {
                            if(Iconfinder.icons.containsKey(basename)) {
                                try {
                                    Tex tex = Resource.remote().loadwait(Iconfinder.icons.get(basename)).layer(Resource.imgc).tex();
                                    g.image(tex, p2c(gob.rc).sub(tex.sz().mul(iconZoom).div(4)).add(delta), tex.dim.mul(iconZoom/2));
                                } catch(Resource.Loading l) {

                                }
                            } else {
                                g.image(bushicn, p2c(gob.rc).add(delta).sub(bldricn.sz().div(2)));
                            }
                        }
                    } else if (gob.type == Gob.Type.TREE || gob.type == Gob.Type.OLDTRUNK) {
                        CheckListboxItem itm = Config.trees.get(basename);
                        if (itm != null && itm.selected) {
                            if(Iconfinder.icons.containsKey(basename)) {
                                try {
                                    Tex tex = Resource.remote().loadwait(Iconfinder.icons.get(basename)).layer(Resource.imgc).tex();
                                    g.image(tex, p2c(gob.rc).sub(tex.sz().mul(iconZoom).div(4)).add(delta), tex.dim.mul(iconZoom/2));
                                } catch(Resource.Loading l) {

                                }
                            } else {
                                g.image(treeicn, p2c(gob.rc).add(delta).sub(bldricn.sz().div(2)));
                            }
                        }
                    }
                    if(res.name.startsWith("gfx/tiles/ridges/cave") && !sentMapGobs.contains(gob.id)) {
						MCache.Grid grd = mv.glob.map.grids.get(gob.rc.div(11*100).floor());
						if(grd != null && Config.pastaMapper) {
							Mapper.sendMarkerData(grd.id, gob.rc.mod(new Coord2d(11*100, 11*100)).div(11).floor().x, gob.rc.mod(new Coord2d(11*100, 11*100)).div(11).floor().y, "gfx/hud/mmap/cave", "Cave");
							sentMapGobs.add(gob.id);
						}
					}

                    if (sgobs.contains(gob.id))
                        continue;

                    if((gob.knocked == null || gob.knocked == Boolean.FALSE) && AlarmManager.play(res.name))
                        sgobs.add(gob.id);

                    /*
                    if (gob.type == Gob.Type.FU_YE_CURIO) {
                        sgobs.add(gob.id);
                        Audio.play(foragablesfx, Config.alarmonforagablesvol);
                    } else if (Config.alarmlocres && gob.type == Gob.Type.LOC_RESOURCE) {
                        sgobs.add(gob.id);
                        Audio.play(swagsfx, Config.alarmlocresvol);
                    } else if (gob.type == Gob.Type.BEAR && gob.knocked == Boolean.FALSE) {
                        sgobs.add(gob.id);
                        Audio.play(bearsfx, 0.6);
                    } else if (gob.type == Gob.Type.LYNX && gob.knocked == Boolean.FALSE) {
                        sgobs.add(gob.id);
                        Audio.play(lynxfx, 0.7);
                    } else if (gob.type == Gob.Type.WALRUS && gob.knocked == Boolean.FALSE) {
                        sgobs.add(gob.id);
                        Audio.play(walrusfx, 0.6);
                    } else if (gob.type == Gob.Type.SEAL && gob.knocked == Boolean.FALSE) {
                        sgobs.add(gob.id);
                        Audio.play(sealsfx, 0.6);
                    } else if (gob.type == Gob.Type.TROLL && gob.knocked == Boolean.FALSE && Config.alarmtroll) {
                        sgobs.add(gob.id);
                        Audio.play(trollsfx, Config.alarmtrollvol);
                    } else if (gob.type == Gob.Type.MAMMOTH && gob.knocked == Boolean.FALSE) {
                        sgobs.add(gob.id);
                        Audio.play(mammothsfx, 0.6);
                    } else if (gob.type == Gob.Type.EAGLE && gob.knocked == Boolean.FALSE) {
                        sgobs.add(gob.id);
                        Audio.play(eaglesfx);
                    } else if (Config.alarmbram && gob.type == Gob.Type.SIEGE_MACHINE) {
                        sgobs.add(gob.id);
                        Audio.play(doomedsfx, Config.alarmbramvol);
                    } else if (gob.type == Gob.Type.WOLF && gob.knocked == Boolean.FALSE) {
                        sgobs.add(gob.id);
                        Audio.play(wolfsfx, 0.6);
                    }
						*/
                } catch (Loading l) {
                }
            }

            for (Gob gob : dangergobs) {
                try {
                    if (gob.type == Gob.Type.PLAYER) {
                        if (ui.sess.glob.party.memb.containsKey(gob.id))
                            continue;

                        Coord pc = p2c(gob.rc).add(delta);

                        KinInfo kininfo = gob.getattr(KinInfo.class);
                        if (pc.x >= 0 && pc.x <= sz.x && pc.y >= 0 && pc.y < sz.y) {
                            g.chcolor(Color.BLACK);
                            g.fcircle(pc.x, pc.y, 5, 16);
                            g.chcolor(kininfo != null ? BuddyWnd.gc[kininfo.group] : Color.WHITE);
                            g.fcircle(pc.x, pc.y, 4, 16);
                            g.chcolor();
                        }

                        if (sgobs.contains(gob.id))
                            continue;

                        boolean enemy = false;
                        if (Config.alarmunknown && kininfo == null) {
                            sgobs.add(gob.id);
                            Audio.play(alarmplayerWhite, Config.alarmunknownvol);
                            enemy = true;
                        } else if (Config.alarmred && kininfo != null && kininfo.group == 2) {
                            sgobs.add(gob.id);
                            Audio.play(alarmplayerRed, Config.alarmredvol);
                            enemy = true;
                        }

                        if (Config.autologout && enemy)
                            gameui().act("lo");
                        else if (Config.autohearth && enemy)
                            gameui().act("travel", "hearth");
                    } else {
                        GobIcon icon = gob.getattr(GobIcon.class);
                        Tex tex;
                        if (icon != null)
                            tex = gob.knocked == Boolean.TRUE ? icon.texgrey() : icon.tex();
                        else
                            tex = Config.additonalicons.get(gob.getres().name);
                        g.image(tex, p2c(gob.rc).sub(tex.sz().mul(iconZoom).div(2)).add(delta), tex.dim.mul(iconZoom));
                    }
                } catch (Loading l) {
                }
            }
        }
    }

    public Gob findicongob(Coord c) {
        OCache oc = ui.sess.glob.oc;
        synchronized (oc) {
            for (Gob gob : oc) {
                try {
                    GobIcon icon = gob.getattr(GobIcon.class);
                    if (icon != null) {
                        Coord gc = p2c(gob.rc);
                        Coord sz = icon.tex().sz();
                        if (c.isect(gc.sub(sz.div(2)), sz)) {
                            Resource res = icon.res.get();
                            CheckListboxItem itm = Config.icons.get(res.basename());
                            if (itm == null || !itm.selected)
                                return gob;
                        }
                    } else { // custom icons
                        Coord gc = p2c(gob.rc);
                        Coord sz = new Coord(18, 18);
                        if (c.isect(gc.sub(sz.div(2)), sz)) {
                            Resource res = gob.getres();
                            if (res != null && Config.additonalicons.containsKey(res.name)) {
                                CheckListboxItem itm = Config.icons.get(res.basename());
                                if (itm == null || !itm.selected)
                                    return gob;
                            }
                        }
                    }
                } catch (Loading l) {
                }
            }
        }
        return (null);
    }

    public void tick(double dt) {
        Gob pl = ui.sess.glob.oc.getgob(MapView.plgob);
        if(pl == null)
            this.cc = mv.cc.floor(tilesz);
        else
            this.cc = pl.rc.floor(tilesz);

        if (Config.playerposfile != null && MapGridSave.gul != null) {
            try {
                // instead of synchronizing MapGridSave.gul we just handle NPE
             //   plcrel = pl.rc.sub((MapGridSave.gul.x + 50) * tilesz.x, (MapGridSave.gul.y + 50) * tilesz.y);
            } catch (NullPointerException npe) {
            }
        }
    }

    public void draw(GOut g) {
        if (cc == null)
            return;

        map:
        {
            final MCache.Grid plg;
            try {
                plg = ui.sess.glob.map.getgrid(cc.div(cmaps));
            } catch (Loading l) {
                break map;
            }
            final int seq = plg.seq;
            
            if (cur == null || plg != cur.grid || seq != cur.seq) {
                Defer.Future<MapTile> f;
                synchronized (cache) {
                    f = cache.get(new Pair<>(plg, seq));
                    if (f == null) {
                        if (cur != null && plg != cur.grid) {
                            int x = Math.abs(plg.gc.x);
                            int y = Math.abs(plg.gc.y);
                            if ((x == 0 && y == 0 || x == 10 && y == 10) && lastnewgid != plg.id) {
                                maptiles.clear();
                                lastnewgid = plg.id;
                            }
                        }
                        f = Defer.later(() -> {
                            Coord ul = plg.ul;
                            Coord gc = plg.gc;
                            BufferedImage[] texes = new BufferedImage[256];
                            maptiles.put(gc.add(-1, -1), drawmap(ul.add(-100, -100), texes));
                            maptiles.put(gc.add(0, -1), drawmap(ul.add(0, -100), texes));
                            maptiles.put(gc.add(1, -1), drawmap(ul.add(100, -100), texes));
                            maptiles.put(gc.add(-1, 0), drawmap(ul.add(-100, 0), texes));
                            maptiles.put(gc, drawmap(ul, texes));
                            maptiles.put(gc.add(1, 0), drawmap(ul.add(100, 0), texes));
                            maptiles.put(gc.add(-1, 1), drawmap(ul.add(-100, 100), texes));
                            maptiles.put(gc.add(0, 1), drawmap(ul.add(0, 100), texes));
                            maptiles.put(gc.add(1, 1), drawmap(ul.add(100, 100), texes));
                            return new MapTile(plg, seq);
                        });
                        cache.put(new Pair<>(plg, seq), f);
                    }
                }
                if (f.done()) {
                    cur = f.get();
                    MapFile save = this.save;
                    if(save != null)
                        save.update(ui.sess.glob.map, cur.grid.gc);
                }
            }
        }
        if (cur != null) {        	
    		int tileSize = (int)(100 * zoom);
    		Coord ts = new Coord(tileSize, tileSize);
    		int hhalf = sz.x / 2;
            int vhalf = sz.y / 2;
            int ht = (hhalf / tileSize) + 2;
            int vt = (vhalf / tileSize) + 2;
            int pox = (int)((cur.grid.gc.x * 100 - cc.x) * zoom) + hhalf + delta.x;
            int poy = (int)((cur.grid.gc.y * 100 - cc.y) * zoom) + vhalf + delta.y;
            int tox = pox / 100 - 1;
            int toy = poy / 100 - 1;
            
            if (maptiles.size() >= 9) {
                for (int x = -ht; x < ht + ht; x++) {
                    for (int y = -vt; y < vt + vt; y++) {
                        Tex mt = maptiles.get(cur.grid.gc.add(x - tox, y - toy));
                        if (mt != null) {
                            int mtcx = (x - tox) * tileSize + pox;
                            int mtcy = (y - toy) * tileSize + poy;
                            if (mtcx + tileSize < 0 || mtcx > sz.x || mtcy + tileSize < 0 || mtcy > sz.y)
                                continue;
                            Coord mtc = new Coord(mtcx, mtcy);
                            g.image(mt, mtc, ts);
                            if (Config.mapshowgrid)
                                g.image(gridred, mtc, ts);
                        }
                    }
                }
            }
        	
            g.image(resize, sz.sub(resize.sz()));

            if (Config.mapshowviewdist) {
                Gob player = mv.player();
                if (player != null) {
					Coord pos = p2c(player.rc.floor().div(100).mul(new Coord2d(100, 100)).add(45, 45)).add(delta).sub((int) (82/2 * zoom), (int) (82/2 * zoom));
					g.image(loadedGrid, pos, new Coord(84, 84).mul(zoom));
				}
            }
        }
        drawicons(g);

        synchronized (ui.sess.glob.party.memb) {
            Collection<Party.Member> members = ui.sess.glob.party.memb.values();
            for (Party.Member m : members) {
                Coord2d ppc;
                Coord ptc;
                double angle;
                try {
                    ppc = m.getc();
                    if (ppc == null) // chars are located in different worlds
                        continue;

                    ptc = p2c(ppc).add(delta);
                    Gob gob = m.getgob();
                    // draw 'x' if gob is outside of view range
                    if (gob == null) {
                        Tex tex = xmap.get(m.col);
                        if (tex == null) {
                            tex = Text.renderstroked("\u2716",  m.col, Color.BLACK, Text.num12boldFnd).tex();
                            xmap.put(m.col, tex);
                        }
                        g.image(tex, ptc.sub(6, 6));
                        continue;
                    }

                    angle = gob.geta();
                } catch (Loading e) {
                    continue;
                }

                final Coord front = new Coord(8, 0).rotate(angle).add(ptc);
                final Coord right = new Coord(-5, 5).rotate(angle).add(ptc);
                final Coord left = new Coord(-5, -5).rotate(angle).add(ptc);
                final Coord notch = new Coord(-2, 0).rotate(angle).add(ptc);
                g.chcolor(m.col);
                g.poly(front, right, notch, left);
                g.chcolor(Color.BLACK);
                g.polyline(1, front, right, notch, left);
                g.chcolor();
            }
        }
    }

    public void center() {
        delta = Coord.z;
    }

    public boolean mousedown(Coord c, int button) {
        if (button != 2) {
            if (cc == null)
                return false;
            Coord csd = c.sub(delta);
            Coord2d mc = c2p(csd);
            if (button == 1)
                MapView.pllastcc = mc;
            Gob gob = findicongob(csd);
            ui.gui.map.stopWalker = true;
            if (gob == null) {
                mv.wdgmsg("click", rootpos().add(csd), mc.floor(posres), button, ui.modflags());
            } else {
                mv.wdgmsg("click", rootpos().add(csd), mc.floor(posres), button, ui.modflags(), 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
                if (Config.autopickmussels && gob.type == Gob.Type.MUSSEL)
                    mv.startMusselsPicker(gob);
            }
        } else if (button == 2) {
            doff = c;
            dragging = ui.grabmouse(this);
        }
        return true;
    }

    public void mousemove(Coord c) {
    	if (this.dragging != null) {
            if (this.tooltip != null) {
                this.tooltip = null;
            }
            this.delta = this.delta.add(c.sub(this.doff));
            this.doff = c;
        } else if (this.cc != null) {
            StringBuilder sb = new StringBuilder();
            Coord pos = c.sub(this.delta);
            Gob gob = this.findicongob(pos);
            if (gob != null) {
                Composite cmp = null;
                GobIcon gi = null;
                GobHealth gh = null;
                for (Map.Entry<Class<? extends GAttrib>, GAttrib> entry : gob.attr.entrySet()) {
                    if (entry.getValue() instanceof Composite) {
                        cmp = (Composite)entry.getValue();
                        continue;
                    }
                    if (entry.getValue() instanceof GobIcon) {
                        gi = (GobIcon)entry.getValue();
                        continue;
                    }
                    if (!(entry.getValue() instanceof GobHealth)) continue;
                    gh = (GobHealth)entry.getValue();
                }
                Resource.Tooltip tt = null;
                if (cmp != null) {
                    tt = (Resource.Tooltip)((Resource)cmp.base.get()).layer(Resource.Tooltip.class);
                }
                if (tt != null) {
                    sb.append(tt.t);
                } else {
                    if (gi != null) {
                        tt = (Resource.Tooltip)((Resource)gi.res.get()).layer(Resource.Tooltip.class);
                    }
                    if (tt != null) {
                        sb.append(tt.t);
                    } else if (cmp != null) {
                        sb.append(((Resource)cmp.base.get()).basename());
                    } else if (gi != null) {
                        sb.append(((Resource)gi.res.get()).basename());
                    }
                }
                if (sb.toString().length() > 0 && gh != null) {
                    sb.append(" [hp:").append(gh.hp).append("]");
                }
            }
            this.tooltip = sb.toString();
        }
    }

    public boolean mouseup(Coord c, int button) {
        if (dragging != null) {
            dragging.remove();
            dragging = null;
        }
        return (true);
    }
    
    public boolean mousewheel(Coord c, int amount) {
    	if (amount > 0 && zoom > 1)
    		zoom = Math.round(zoom * 100 - 20) / 100f;
    	else if (amount < 0 && zoom < 3) 
    		zoom = Math.round(zoom * 100 + 20) / 100f;
    	
    	iconZoom = Math.round((zoom - 1) * 100 / 2) / 100f + 1;
    	return true;
    }
}