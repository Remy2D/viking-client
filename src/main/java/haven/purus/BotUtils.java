package haven.purus;

import haven.*;
import haven.FlowerMenu.Petal;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static haven.OCache.posres;

public class BotUtils {

	public static GameUI gui;
	private static Pattern liquidPattern = Pattern.compile(String.format("[0-9.]+ l of (%s)",
			String.join("|", new String[] { "Water", "Piping Hot Tea", "Tea" }), Pattern.CASE_INSENSITIVE));

	// Click some object with button and a modifier
	// Button 1 = Left click and 3 = right click
	// Modifier 1 - shift; 2 - ctrl; 4 - alt;
	public static void doClick(Gob gob, int button, int mod) {
		gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), button, 0, mod, (int) gob.id, gob.rc.floor(posres), 0,
				-1);
	}
	
	public static int getAmount(GItem item) {
		int ret = -1;
		synchronized(item.ui) {
			List<ItemInfo> li = item.info();
			if(li == null)
				return -1;
			for(ItemInfo o : li) {
				if(o instanceof GItem.Amount)
					ret = ((GItem.Amount) o).itemnum();
			}
		}
		return ret;
	}
	
	// Returns null if not found
	public static Coord getFreeInvSlot(Inventory inventory) {
		return inventory.getFreeSlot();
	}

	// Finds nearest objects and returns closest one
	public static Gob findObjectByNames(int radius, String... names) {
		Coord2d plc = player().rc;
		double min = radius;
		Gob nearest = null;
		synchronized (gui.ui.sess.glob.oc) {
			for (Gob gob : gui.ui.sess.glob.oc) {
				double dist = gob.rc.dist(plc);
				if (dist < min) {
					boolean matches = false;
					for (String name : names) {
						try {
							if(gob.getres() != null && gob.getres().name.equals(name)) {
								matches = true;
								break;
							}
						} catch(Session.LoadingIndir l) {

						}
					}
					if (matches) {
						min = dist;
						nearest = gob;
					}
				}
			}
		}
		return nearest;
	}

	// Find object by ID, returns null if not found
	public static Gob findObjectById(long id) {
		return gui.ui.sess.glob.oc.getgob(id);
	}

	// true if player moving
	public static boolean isMoving() {
		if (player().getv() == 0)
			return false;
		else
			return true;
	}

	// Replace with pasta pathfinder
	/*// Right clicks a gob with pathfinder (Pathfinds near, then right clicks)
	public static void pfRightClick(Gob gob, int mod) {
		gui.map.pfRightClick(gob, -1, 3, mod, null);
	}*/

	// Chooses option from flower menu
	public static void Choose(Petal option) {
		gui.wdgmsg("cl", option.num, gui.ui.modflags());
	}

	// Click some object with an item on hand
	// Modifier 1 - shift; 2 - ctrl; 4 alt;
	public static void itemClick(Gob gob, int mod) {
		gui.map.wdgmsg("itemact", Coord.z, gob.rc.floor(posres), mod, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
	}

	// Returns players gob
	public static Gob player() {
		return gui.map.player();
	}

	// Sends message to the user
	public static void sysMsg(String str, Color col) {
		gui.msg(str, col);
	}

	// Drinks liquids from containers in inventory
	public static void drink() {
		GameUI gui = HavenPanel.lui.root.findchild(GameUI.class);
		IMeter.Meter stam = gui.getmeter("stam", 0);
		WItem item = null;
		do {
			item = findDrink(playerInventory());

			if(item != null) {
				item.item.wdgmsg("iact", Coord.z, 3);
				while(gui.ui.root.findchild(FlowerMenu.class) == null)
					sleep(10);
				@SuppressWarnings("deprecation") FlowerMenu menu = gui.ui.root.findchild(FlowerMenu.class);
				if(menu != null) {
					for(FlowerMenu.Petal opt : menu.opts) {
						if(opt.name.equals("Drink")) {
							menu.choose(opt);
							menu.destroy();
							sleep(500);
							while(gui.prog >= 0) {
								sleep(100);
							}
						}
					}
				}
			}
		} while(item != null && stam.a <= 80);
	}

	// Finds an item from inventory that contains liquids that can be consumed
	public static WItem findDrink(Inventory inv) {
		for (WItem item : inv.children(WItem.class)) {
			if (canDrinkFrom(item))
				return item;
		}
		Equipory e = gui.getequipory();
		WItem l = e.quickslots[6];
		WItem r = e.quickslots[7];
		if (canDrinkFrom(l))
			return l;
		if (canDrinkFrom(r))
			return r;
		return null;
	}

	// Returns true if player can drink from the item
	public static boolean canDrinkFrom(WItem item) {
		ItemInfo.Contents contents = getContents(item);
		if (contents != null && contents.sub != null) {
			synchronized(item.item.ui) {
				for(ItemInfo info : contents.sub) {
					if(info instanceof ItemInfo.Name) {
						ItemInfo.Name name = (ItemInfo.Name) info;
						if(name.str != null && liquidPattern.matcher(name.str.text).matches())
							return true;
					}
				}
			}
		}
		return false;
	}

	// Returns contents of an item
	public static ItemInfo.Contents getContents(WItem item) {
		try {
			synchronized(item.ui) {
				for(ItemInfo info : item.item.info())
					if(info instanceof ItemInfo.Contents)
						return (ItemInfo.Contents) info;
			}
		} catch (Loading ignored) {
		}
		return null;
	}

	// Returns players inventory
	public static Inventory playerInventory() {
		return gui.maininv;
	}

	// Takes item in hand
	public static void takeItem(Widget item) {
		item.wdgmsg("take", Coord.z);
		while (getItemAtHand() == null) {
			sleep(10);
		}
	}

	// Returns item in hand
	public static GItem getItemAtHand() {
		for (GameUI.DraggedItem item : gui.hand)
			return item.item;
		return null;
	}

	// Waits for t milliseconds
	public static void sleep(int t) {
		try {
			Thread.sleep(t);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// Finds the nearest crop with a name and stage
	public static Gob findNearestStageCrop(int radius, int stage, String... names) {
		Coord2d plc = player().rc;
		double min = radius;
		Gob nearest = null;
		synchronized (gui.ui.sess.glob.oc) {
			for (Gob gob : gui.ui.sess.glob.oc) {
				double dist = gob.rc.dist(plc);
				if (dist < min) {
					boolean matches = false;
					for (String name : names) {
						if (true) {
							if (gob.getStage() == stage) {
								matches = true;
								break;
							}
						}
					}
					if (matches) {
						min = dist;
						nearest = gob;
					}
				}
			}
		}
		return nearest;
	}

	// Checks if the object's name can be found from resources
	public static boolean isObjectName(Gob gob, String name) {
		try {
			Resource res = gob.getres();
			return (res != null) && res.name.contains(name);
		} catch (Loading e) {
			return false;
		}
	}

	// Use item in hand to ground below player, for example, plant carrot
	public static void mapInteractClick(int mod) {
		gui.map.wdgmsg("itemact", getCenterScreenCoord(), player().rc.floor(posres), mod);
	}

	// Destroys the given gob
	public static void destroyGob(Gob gob) {
		gui.menu.wdgmsg("act", new Object[] { "destroy" });
		doClick(gob, 1, 0);
	}

	// Drops an item from the hand
	public static void dropItem(int mod) {
		gui.map.wdgmsg("drop", Coord.z, gui.map.player().rc.floor(posres), mod);
	}

	// Returns the coordinates of the center of the screen
	public static Coord getCenterScreenCoord() {
		Coord sc, sz;
		sz = gui.map.sz;
		sc = new Coord((int) Math.round(Math.random() * 200 + sz.x / 2 - 100),
				(int) Math.round(Math.random() * 200 + sz.y / 2 - 100));
		return sc;
	}

	// Waits for window to appear
	public static void waitForWindow(String windowName) {
		while (gui.getwnd(windowName) == null) {
			sleep(10);
		}
	}

	// Returns witems with specific names from inventory
	public static List<WItem> getInventoryItemsByNames(Inventory invwdg, List<String> items) {
		List<WItem> witems = new ArrayList<WItem>();
		for (WItem wi : getInventoryContents(invwdg)) {
			String resname = wi.item.resource().name;
			for (String s : items) {
				if (resname.equals(s))
					witems.add(wi);
			}
		}
		return witems;
	}

	// Returns witems with specific name from inventory
	public static List<WItem> getInventoryItemsByName(Inventory invwdg, String item) {
		List<WItem> witems = new ArrayList<WItem>();
		for (WItem wi : getInventoryContents(invwdg)) {
			String resname = wi.item.resource().name;
			if (resname.equals(item))
				witems.add(wi);
		}
		return witems;
	}

	// Returns all items that inventory contains
	public static List<WItem> getInventoryContents(Inventory invwdg) {
		List<WItem> witems = new ArrayList<WItem>();
		for (Widget witm = invwdg.lchild; witm != null; witm = witm.prev) {
			if (witm instanceof WItem) {
				witems.add((WItem) witm);
			}
		}
		return witems;
	}

	// Logout to char selection
	public static void logoutChar() {
		gui.act("lo", "cs");
	}
	
	// Drops item from  hand to given slot in given inventory
	public static void dropItemToInventory(Coord coord, Inventory inventory) {
		inventory.wdgmsg("drop", coord);
	}
	
	// Returns true if stockpile is full, might not work for all stockpiles
	public static boolean stockpileIsFull(Gob gob) {
		if(gob.getattr(ResDrawable.class).sdt.peekrbuf(0)==31)
			return true;
		else
			return false;	
	}

	// Repeat shift + itemact until all similari tems from inventory have been clicked
	// May result in valuable items accidentally being put in the trough etc.
	public static void itemClickAll(Gob gob) {
		Object[] args = {Coord.z, gob.rc.floor(posres), 1, 0, (int) gob.id, gob.rc.floor(posres), 0, -1};
		gui.map.lastItemactClickArgs = args;
		gui.map.wdgmsg("itemact", Coord.z, gob.rc.floor(posres), 1, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
	}


	// Returns amount of free inventory slots
	public static int invFreeSlots() {
		int takenSlots = 0;
		for (Widget i = playerInventory().child; i != null; i = i.next) {
			if (i instanceof WItem) {
				WItem buf = (WItem) i;
				takenSlots += buf.size().x * buf.size().y;
			}
		}
		int allSlots = playerInventory().isz.x * playerInventory().isz.y;
		return allSlots - takenSlots;
	}

	public static void pfLeftClick(Coord mc) {
		gui.map.purusPfLeftClick(mc, "");
		try {
			gui.map.pastaPathfinder.join();
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void pfGobClick(Gob gob, int btn, int mod) {
		gui.map.purusPfRightClick(gob, -1, btn, mod, "");
		try {
			gui.map.pastaPathfinder.join();
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
}