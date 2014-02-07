package li.cil.oc.driver;

import cpw.mods.fml.common.Loader;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class Registry {
    private static final Set<IModHandler> handlers = new HashSet<IModHandler>();

    private Registry() {
    }

    public static void add(final IModHandler mod) {
        final boolean alwaysEnabled = mod.getModId() == null || mod.getModId().isEmpty();
        if ((alwaysEnabled || Loader.isModLoaded(mod.getModId())) && handlers.add(mod)) {
            // TODO Log message? Use logger instead of println if so.
            mod.initialize();
        }
    }

    public static Map<String, Object> toMap(final ItemStack value) {
        if (value == null) {
            return null;
        }
        final Map<String, Object> map = new HashMap<String, Object>();
        for (IModHandler handler : handlers) {
            handler.populate(map, value);
        }
        return map;
    }
}
