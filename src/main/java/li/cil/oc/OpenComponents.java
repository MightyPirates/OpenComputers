package li.cil.oc;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import li.cil.oc.driver.Registry;
import li.cil.oc.driver.buildcraft.HandlerBuildCraft;
import li.cil.oc.driver.enderstorage.HandlerEnderStorage;
import li.cil.oc.driver.thermalexpansion.HandlerThermalExpansion;
import li.cil.oc.driver.vanilla.HandlerVanilla;

@Mod(modid = "OpenComputers|Components", useMetadata = true)
public class OpenComponents {
    @Mod.Instance
    public static OpenComponents instance;

    @Mod.EventHandler
    public void init(final FMLInitializationEvent e) {
        Registry.add(new HandlerBuildCraft());
        Registry.add(new HandlerEnderStorage());
        Registry.add(new HandlerThermalExpansion());
        Registry.add(new HandlerVanilla());
    }
}
