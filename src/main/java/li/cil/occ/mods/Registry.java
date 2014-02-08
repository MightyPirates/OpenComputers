package li.cil.occ.mods;

import cpw.mods.fml.common.Loader;
import li.cil.occ.OpenComponents;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public final class Registry {
    private static final Set<IMod> handlers = new HashSet<IMod>();

    private Registry() {
    }

    public static void add(final IMod mod) {
        final boolean alwaysEnabled = mod.getModId() == null || mod.getModId().isEmpty();
        if ((alwaysEnabled || Loader.isModLoaded(mod.getModId())) && handlers.add(mod)) {
            OpenComponents.Log.info(String.format("Initializing handler for '%s'.", mod.getModId()));
            try {
                mod.initialize();
            } catch (Throwable e) {
                OpenComponents.Log.log(Level.WARNING, String.format("Error initializing handler for '%s'", mod.getModId()), e);
            }
        }
    }

    public static Map<String, Object> toMap(final ItemStack value) {
        if (value == null) {
            return null;
        }
        final Map<String, Object> map = new HashMap<String, Object>();
        for (IMod handler : handlers) {
            handler.populate(map, value);
        }
        return map;
    }
}
