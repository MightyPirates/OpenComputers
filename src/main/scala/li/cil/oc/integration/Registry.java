package li.cil.oc.integration;

import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.util.mods.Mods;

import java.util.HashSet;
import java.util.Set;

public final class Registry {
    private static final Set<IMod> handlers = new HashSet<IMod>();

    private Registry() {
    }

    public static void add(final IMod mod) {
        final boolean isBlacklisted = Settings.get().modBlacklist().contains(mod.getMod().id());
        final boolean alwaysEnabled = mod.getMod() == null || mod == Mods.Minecraft();
        if (!isBlacklisted && (alwaysEnabled || mod.getMod().isAvailable()) && handlers.add(mod)) {
            OpenComputers.log().info(String.format("Initializing converters and drivers for '%s'.", mod.getMod().id()));
            try {
                mod.initialize();
            } catch (Throwable e) {
                OpenComputers.log().warn(String.format("Error initializing handler for '%s'", mod.getMod().id()), e);
            }
        }
    }
}
