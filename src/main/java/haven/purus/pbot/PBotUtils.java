package haven.purus.pbot;

import haven.Window;
import haven.*;
import haven.purus.BotUtils;
import haven.purus.DrinkWater;
import haven.purus.ItemClickCallback;
import haven.purus.pbot.gui.PBotWindow;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

import static haven.OCache.posres;

public class PBotUtils {

	private static Coord selectedAreaA, selectedAreaB;
	private static boolean itemSelectWait;
	private static PBotItem selectedItem;

	/**
	 * Sleep for t milliseconds
	 * @param t Time to wait in milliseconds
	 */
	public static void sleep(int t) {
		try {
			Thread.sleep(t);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Right click a gob with pathfinder, wait until pathfinder is finished
	 * @param gob Gob to right click
	 * @param mod 1 = shift, 2 = ctrl, 4 = alt
	 * @return False if path was not found, true if it was found
	 */
	public static boolean pfRightClick(PBotGob gob, int mod) {
		PBotAPI.gui.map.purusPfRightClick(gob.gob, -1, 3, mod, null);
		try {
			PBotAPI.gui.map.pastaPathfinder.join();
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		synchronized(PBotAPI.gui.map) {
			return PBotAPI.gui.map.foundPath;
		}
	}

	/**
	 * Chooses a petal with given label from a flower menu that is currently open
	 * @param name Name of petal to open
	 * @return False if petal or flower menu with name could not be found
	 */
	public static boolean choosePetal(String name) {
		FlowerMenu menu = PBotAPI.gui.ui.root.findchild(FlowerMenu.class);
		if(menu != null) {
			for(FlowerMenu.Petal opt : menu.opts) {
				if(opt.name.equals(name)) {
					menu.choose(opt);
					menu.destroy();
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Wait for the flowermenu to close
	 */
	public static void waitFlowermenuClose() {
		while(PBotAPI.gui.ui.root.findchild(FlowerMenu.class) != null)
			sleep(25);
	}

	/**
	 * Click some place on map
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @param btn 1 = left click, 3 = right click
	 * @param mod 1 = shift, 2 = ctrl, 4 = alt
	 */
	public static void mapClick(int x, int y, int btn, int mod) {
		PBotAPI.gui.map.wdgmsg("click", getCenterScreenCoord(), new Coord2d(x, y).floor(posres), btn, mod);
	}

	/**
	 * Use item in hand to ground below player, for example, to plant carrot
	 */
	public static void mapInteractClick() {
		PBotAPI.gui.map.wdgmsg("itemact", PBotUtils.getCenterScreenCoord(), PBotGobAPI.player().getRcCoords().floor(posres), 3, PBotAPI.gui.ui.modflags());
	}

	/**
	 * Coordinates of the center of the screen
	 * @return Coordinates of the center of the screen
	 */
	public static Coord getCenterScreenCoord() {
		Coord sc, sz;
		sz = PBotAPI.gui.map.sz;
		sc = new Coord((int) Math.round(Math.random() * 200 + sz.x / 2 - 100),
				(int) Math.round(Math.random() * 200 + sz.y / 2 - 100));
		return sc;
	}

	/**
	 * Left click to somewhere with pathfinder, wait until pathfinder is finished
	 * @param x X-Coordinate
	 * @param y Y-Coordinate
	 * @return False if path was not found, true if it was found
	 */
	public static boolean pfLeftClick(double x, double y) {
		PBotAPI.gui.map.purusPfLeftClick(new Coord2d(x, y), null);
		try {
			PBotAPI.gui.map.pastaPathfinder.join();
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		synchronized(PBotAPI.gui.map) {
			return PBotAPI.gui.map.foundPath;
		}
	}

	/**
	 * Starts crafting item with the given name
	 * @param name Name of the item ie. "clogs"
	 * @param makeAll 0 To craft once, 1 to craft all
	 */
	public static void craftItem(String name, int makeAll) {
		openCraftingWnd(name);
		loop:
		while(true) {
			for(Widget w : PBotAPI.gui.ui.widgets.values()) {
				if (w instanceof Makewindow) {
					PBotAPI.gui.wdgmsg(w, "make", makeAll);
					break loop;
				}
			}
			sleep(25);
		}
	}

	/**
	 * Waits for flower menu to appear
	 */
	public static void waitForFlowerMenu() {
		while(PBotAPI.gui.ui.root.findchild(FlowerMenu.class) == null) {
			BotUtils.sleep(15);
		}
	}

	/**
	 * Waits for the flower menu to disappear
	 */
	public static void closeFlowermenu() {
		FlowerMenu menu = PBotAPI.gui.ui.root.findchild(FlowerMenu.class);
		if(menu != null) {
			menu.choose(null);
			menu.destroy();
		}
		while(PBotAPI.gui.ui.root.findchild(FlowerMenu.class) != null) {
			BotUtils.sleep(15);
		}
	}

	/**
	 * Waits for the hourglass timer when crafting or drinking for example
	 * Also waits until the hourglass has been seen to change at least once
	 */
	public static void waitForHourglass() {
		double prog = PBotAPI.gui.prog;
		while (prog == PBotAPI.gui.prog) {
			prog = PBotAPI.gui.prog;
			sleep(5);
		}
		while (PBotAPI.gui.prog >= 0) {
			sleep(50);
		}
	}

	/**
	 * Waits for the hourglass timer when crafting or drinking for example
	 * Also waits until the hourglass has been seen to change at least once
	 * If hourglass does not appear within timeout, returns false, else true
	 * @param timeout Timeout in milliseconds
	 */
	public static boolean waitForHourglass(int timeout) {
		double prog = PBotAPI.gui.prog;
		int retries = 0;
		while(prog == PBotAPI.gui.prog) {
			if(retries > timeout/5)
				return false;
			retries++;
			prog = PBotAPI.gui.prog;
			sleep(5);
		}
		while (PBotAPI.gui.prog >= 0) {
			sleep(25);
		}
		return true;
	}

	/**
	 * Returns value of hourglass, -1 = no hourglass, else the value between 0.0 and 1.0
	 * @return value of hourglass
	 */
	public static double getHourglass() {
		return PBotAPI.gui.prog;
	}

	// TODO: Return false if drinking was not successful (no water found for example)
	/**
	 * Attempts to drink water by using the same water drinking script as in extensions
	 * @param wait Wait for the drinking to finish
	 */
	public static boolean drink(boolean wait) {
		if(!PBotAPI.gui.drinkingWater) {
			Thread t = new Thread(new DrinkWater(PBotAPI.gui));
			t.start();
			if(wait) {
				try {
					t.join();
					if(!PBotAPI.gui.lastDrinkingSucessful) {
						sysMsg("PBotUtils Warning: Couldn't drink, didn't find anything to drink!", Color.ORANGE);
						return false;
					}
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
				waitForHourglass();
			}
		}
		return true;
	}

	/**
	 * Opens the crafting window for given item
	 * @param name Name of craft for wdgmsg
	 */
	public static void openCraftingWnd(String name) {
		// Close current window and wait for it to close
		Window wnd = PBotWindowAPI.getWindow("Crafting");
		if(wnd != null)
			PBotWindowAPI.closeWindow(wnd);
		PBotWindowAPI.waitForWindowClose("Crafting", 1000*1000);
		PBotAPI.gui.wdgmsg("act", "craft", name);
		PBotWindowAPI.waitForWindow("Crafting", 1000*1000);
	}

	/**
	 * Send a system message to the user
	 * @param str Message to send
	 */
	public static void sysMsg(String str) {
		PBotAPI.gui.msg(str, Color.WHITE);
	}

	/**
	 * Send a system message to the user
	 * @param str Message to send
	 * @param col Color of the text
	 */
	public static void sysMsg(String str, Color col) {
		PBotAPI.gui.msg(str, col);
	}

	/**
	 * Send a system message to the user
	 * @param str Message to send
	 * @param r Amount of red colour in the text
	 * @param g Amount of green colour in the text
	 * @param b Amount of blue colour in the text
	 */
	public static void sysMsg(String str, int r, int g, int b) {
		PBotAPI.gui.msg(str, new Color(r, g, b));
	}

	/**
	 * Returns the players inventory
	 * @return Inventory of the player
	 */
	public static PBotInventory playerInventory() {
		return new PBotInventory(PBotAPI.gui.maininv);
	}

	/**
	 * Returns all open inventories
	 * @return List of inventories
	 */
	public static ArrayList<PBotInventory> getAllInventories() {
		ArrayList<PBotInventory> ret = new ArrayList<>();
		for(Widget window = PBotAPI.gui.lchild; window != null; window = window.prev) {
			if(window instanceof Window) {
				for(Widget wdg = window.lchild; wdg != null; wdg = wdg.prev) {
					if(wdg instanceof Inventory) {
						ret.add(new PBotInventory((Inventory) wdg));
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Create a PBotWindow object, See PBotWindow for usage
	 * @param title Title of the window
	 * @param height Height of the window
	 * @param width Width of the window
	 * @param id scriptID variable of the script
	 * @return PBotWindow object
	 */
	public static PBotWindow PBotWindow(String title, int height, int width, String id) {
		PBotWindow window = new PBotWindow(new Coord(width, height), title, id);
		PBotAPI.gui.add(window, 300, 300);
		return window;
	}

	/**
	 * Returns the item currently in the hand
	 * @return Item at hand
	 */
	public static PBotItem getItemAtHand() {
		if(PBotAPI.gui.vhand == null)
			return null;
		else
			return new PBotItem(PBotAPI.gui.vhand);
	}


	/**
	 * Drops an item from the hand and waits until it has been dropped
	 * @param mod 1 = shift, 2 = ctrl, 4 = alt
	 */
	public static void dropItemFromHand(int mod) {
		PBotAPI.gui.map.wdgmsg("drop", Coord.z, PBotAPI.gui.map.player().rc.floor(posres), mod);
		while(getItemAtHand() != null)
			sleep(25);
	}

	/**
	 * Activate area selection by dragging.
	 * To get coordinates of the area selected, use getSelectedAreaA and getSelectedAreaB
	 * User can select an area by dragging
	 */
	public static void selectArea() {
		sysMsg("Please select an area by dragging!", Color.ORANGE);
		PBotAPI.gui.map.PBotAPISelect = true;
		while(PBotAPI.gui.map.PBotAPISelect)
			sleep(25);
	}

	/**
	 * Get A point of the rectangle selected with selectArea()
	 * @return A-Point of the rectangle
	 */
	public static Coord getSelectedAreaA() {
		return selectedAreaA;
	}

	/**
	 * Get B point of the rectangle selected with selectArea()
	 * @return B-Point of the rectangle
	 */
	public static Coord getSelectedAreaB() {
		return selectedAreaB;
	}

	/**
	 * Callback for area select
	 */
	public static void areaSelect(Coord a, Coord b) {
		selectedAreaA = a.mul(MCache.tilesz2);
		selectedAreaB = b.mul(MCache.tilesz2).add(11, 11);
		sysMsg("Area selected!", Color.ORANGE);
	}

	/**
	 * Returns a list of gobs in the rectangle between A and B points
	 * @param a A-point of the rectangle
	 * @param b B-point of the rectangle
	 * @return List of gobs in the area, sorted to zig-zag pattern
	 */
	public static ArrayList<PBotGob> gobsInArea(Coord a, Coord b) {
		// Initializes list of crops to harvest between the selected coordinates
		ArrayList<PBotGob> gobs = new ArrayList<PBotGob>();
		double bigX = a.x > b.x ? a.x : b.x;
		double smallX = a.x < b.x ? a.x : b.x;
		double bigY = a.y > b.y ? a.y : b.y;
		double smallY = a.y < b.y ? a.y : b.y;
		synchronized(PBotAPI.gui.ui.sess.glob.oc) {
			for(Gob gob : PBotAPI.gui.ui.sess.glob.oc) {
				if(gob.rc.x <= bigX && gob.rc.x >= smallX && gob.getres() != null && gob.rc.y <= bigY
						&& gob.rc.y >= smallY) {
					gobs.add(new PBotGob(gob));
				}
			}
		}
		gobs.sort(new CoordSort());
		return gobs;
	}

	/**
	 * Resource name of the tile in the given location
	 * @param x X-Coord of the location (rc coord)
	 * @param y Y-Coord of the location (rc coord)
	 * @return
	 */
	public static String tileResnameAt(int x, int y) {
		try {
			Coord loc = new Coord(x, y);
			int t = PBotAPI.gui.map.glob.map.gettile(loc.div(11));
			Resource res = PBotAPI.gui.map.glob.map.tilesetr(t);
			if(res != null)
				return res.name;
			else
				return null;
		} catch(Loading l) {

		}
		return null;
	}

	// Sorts coordinate array to efficient zig-zag-like sequence for farming etc.
	private static class CoordSort implements Comparator<PBotGob> {
		public int compare(PBotGob a, PBotGob b) {
			if (a.gob.rc.floor().x == b.gob.rc.floor().x) {
				if (a.gob.rc.floor().x % 2 == 0)
					return (a.gob.rc.floor().y <b.gob.rc.floor().y) ? 1 : (a.gob.rc.floor().y >b.gob.rc.floor().y) ? -1 : 0;
				else
					return (a.gob.rc.floor().y <b.gob.rc.floor().y) ? -1 : (a.gob.rc.floor().y >b.gob.rc.floor().y) ? 1 : 0;
			} else
				return (a.gob.rc.floor().x <b.gob.rc.floor().x) ? -1 : (a.gob.rc.floor().x > b.gob.rc.floor().x) ? 1 : 0;
		}
	}

	/**
	 * Next click to item in inventory returns the item, the function will wait until this happens
	 */
	public static PBotItem selectItem() {
		synchronized (ItemClickCallback.class) {
			BotUtils.gui.registerItemCallback(new ItemCb());
		}
		while(itemSelectWait) {
			PBotUtils.sleep(25);
		}
		synchronized(ItemClickCallback.class) {
			BotUtils.gui.unregisterItemCallback();
		}
		return selectedItem;
	}

	private static class ItemCb implements ItemClickCallback {

		public ItemCb() {
			itemSelectWait = true;
		}

		@Override
		public void itemClick(WItem item) {
			selectedItem = new PBotItem(item);
			itemSelectWait = false;
		}
	}
}
