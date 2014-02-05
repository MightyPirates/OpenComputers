package li.cil.oc;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import li.cil.oc.driver.buildcraft.ComponentsBuildCraft;
import li.cil.oc.driver.vanilla.ComponentsVanilla;

@Mod(modid = "OpenComputers|Components", name = "OpenComponents", version = "1.0.0", dependencies = "required-after:OpenComputers@[1.2.0,)")
public class OpenComponents {
    @Mod.Instance
    public static OpenComponents instance;

    @Mod.EventHandler
    public static void init(final FMLInitializationEvent e) {
        ComponentsVanilla.register();
        if (Loader.isModLoaded("BuildCraft|Core")) {
            ComponentsBuildCraft.register();
        }
    }
}
