package mods.railcraft.api.core.items;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.item.ItemStack;

/**
 * This interface is used with several of the functions in IItemTransfer
 * to provide a convenient means of dealing with entire classes of items without
 * having to specify each item individually.
 * @author CovertJaguar <http://www.railcraft.info>
 */
public interface IStackFilter
{
    /**
     * Railcraft adds the following IItemTypes during preInit: ALL, FUEL, TRACK, MINECART, BALLAST, FEED
     *
     * Feel free to grab them from here or define your own.
     */
    public static final Map<String, IStackFilter> filters = new HashMap<String, IStackFilter>();

    public boolean matches(ItemStack stack);
}
