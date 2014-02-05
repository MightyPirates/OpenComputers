package li.cil.oc;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import li.cil.oc.driver.IDriverBundle;
import li.cil.oc.driver.buildcraft.BundleBuildCraft;
import li.cil.oc.driver.enderstorage.BundleEnderStorage;
import li.cil.oc.driver.vanilla.BundleVanilla;

@Mod(modid = "OpenComputers|Components", name = "OpenComponents", version = "1.0.0", dependencies = "required-after:OpenComputers@[1.2.0,)")
public class OpenComponents {
    @Mod.Instance
    public static OpenComponents instance;

    @Mod.EventHandler
    public void init(final FMLInitializationEvent e) {
        register(new BundleVanilla());
        register(new BundleBuildCraft());
        register(new BundleEnderStorage());
    }

    private void register(IDriverBundle bundle) {
        if (bundle.getModId() == null || bundle.getModId().isEmpty() || Loader.isModLoaded(bundle.getModId())) {
            bundle.initialize();
        }
    }
}
