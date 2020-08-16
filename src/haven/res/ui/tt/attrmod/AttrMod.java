package haven.res.ui.tt.attrmod;

import haven.CharWnd;
import haven.Coord;
import haven.ItemInfo;
import haven.PUtils;
import haven.Resource;
import haven.RichText;
import haven.res.ui.tt.Armor;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;

public class AttrMod extends ItemInfo.Tip {
	public final Collection<Mod> mods;

	public AttrMod(ItemInfo.Owner paramOwner, Collection<Mod> paramCollection) {
		super(paramOwner);
		this.mods = paramCollection;
	}

	public static String buff = "128,255,128", debuff = "255,128,128";

	public static BufferedImage modimg(Collection<Mod> paramCollection) {
		ArrayList<BufferedImage> arrayList = new ArrayList(paramCollection.size());
		for (Mod mod : paramCollection) {
			BufferedImage bufferedImage1 = (RichText.render(String.format("%s $col[%s]{%s%d}", new Object[] { ((Resource.Tooltip)mod.attr.layer(Resource.tooltip)).t, (mod.mod < 0) ? debuff : buff,
					(char)((mod.mod < 0) ? 45 : 43), Integer.valueOf(Math.abs(mod.mod)) }), 0, new Object[0])).img;
			BufferedImage bufferedImage2 = PUtils.convolvedown(((Resource.Image)mod.attr.layer(Resource.imgc)).img, new Coord(bufferedImage1
					.getHeight(), bufferedImage1.getHeight()), CharWnd.iconfilter);
			arrayList.add(catimgsh(0, new BufferedImage[] { bufferedImage2, bufferedImage1 }));
		}
		return catimgs(0, arrayList.<BufferedImage>toArray(new BufferedImage[0]));
	}


	public static class Fac implements ItemInfo.InfoFactory {
		public Fac() {

		}
		public ItemInfo build(ItemInfo.Owner paramOwner, Object... paramVarArgs) {
			Resource.Resolver resolver = (Resource.Resolver)paramOwner.context(Resource.Resolver.class);
			ArrayList<AttrMod.Mod> arrayList = new ArrayList();
			for (byte b = 1; b < paramVarArgs.length; b += 2)
				arrayList.add(new AttrMod.Mod((Resource)resolver.getres(((Integer)paramVarArgs[b]).intValue()).get(), ((Integer)paramVarArgs[b + 1]).intValue()));
			return (ItemInfo)new AttrMod(paramOwner, arrayList);
		}
	}


	public static class Mod {
		public final Resource attr;

		public int mod;

		public Mod(Resource paramResource, int paramInt) {
			this.attr = paramResource;
			this.mod = paramInt;
		}
	}


	public BufferedImage tipimg() {
		return modimg(this.mods);
	}
}
