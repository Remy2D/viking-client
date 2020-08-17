package haven.res.ui.tt.slots;

import haven.*;

import java.util.LinkedList;

public class SlotFactory implements ItemInfo.InfoFactory {
	public ItemInfo build(ItemInfo.Owner paramOwner, ItemInfo.Raw paramRaw, Object... paramVarArgs) {
		Resource.Resolver resolver = (Resource.Resolver)paramOwner.context(Resource.Resolver.class);
		byte b = 1;
		double d1 = ((Number)paramVarArgs[b++]).doubleValue();
		double d2 = ((Number)paramVarArgs[b++]).doubleValue();
		LinkedList<Object> linkedList = new LinkedList();
		while (paramVarArgs[b] != null)
			linkedList.add(resolver.getres(((Integer)paramVarArgs[b++]).intValue()).get());
		b++;
		int i = ((Integer)paramVarArgs[b++]).intValue();
		ISlots iSlots = new ISlots(paramOwner, i, d1, d2, linkedList.<Resource>toArray(new Resource[0]));
		while (b < paramVarArgs.length) {
			MessageBuf messageBuf = MessageBuf.nil;
			Indir indir = resolver.getres(((Integer)paramVarArgs[b++]).intValue());
			Message message = Message.nil;
			if (paramVarArgs[b] instanceof byte[])
				messageBuf = new MessageBuf((byte[])paramVarArgs[b++]);
			Object[] arrayOfObject = (Object[])paramVarArgs[b++];
			iSlots.getClass();
			iSlots.s.add(new ISlots.SItem(iSlots, new ResData(indir, (Message)messageBuf), arrayOfObject));
		}
		return (ItemInfo)iSlots;
	}

}
