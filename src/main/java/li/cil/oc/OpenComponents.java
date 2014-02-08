package li.cil.oc;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import li.cil.oc.driver.Registry;
import li.cil.oc.driver.appeng.HandlerAppEng;
import li.cil.oc.driver.atomicscience.HandlerAtomicScience;
import li.cil.oc.driver.buildcraft.HandlerBuildCraft;
import li.cil.oc.driver.computercraft.HandlerComputerCraft;
import li.cil.oc.driver.enderstorage.HandlerEnderStorage;
import li.cil.oc.driver.ic2.HandlerIndustrialCraft2;
import li.cil.oc.driver.mekanism.HandlerMekanism;
import li.cil.oc.driver.redstoneinmotion.HandlerRedstoneInMotion;
import li.cil.oc.driver.thermalexpansion.HandlerThermalExpansion;
import li.cil.oc.driver.vanilla.HandlerVanilla;
import net.minecraftforge.common.Configuration;

import java.util.logging.Logger;

@Mod(modid = "OpenComponents", useMetadata = true)
public class OpenComponents {
    @Mod.Instance
    public static OpenComponents instance;

    public static final Logger Log = Logger.getLogger("OpenComponents");

    public static boolean computerCraftWrapEverything;

    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent e) {
        final Configuration config = new Configuration(e.getSuggestedConfigurationFile());

        computerCraftWrapEverything = config.
                get("computercraft", "wrapEverything", computerCraftWrapEverything, "" +
                        "Enable this to automatically make any methods other mods'\n" +
                        "blocks make available to ComputerCraft available via the\n" +
                        "Adapter. BEWARE: this is disabled by default for a good\n" +
                        "reason - this will not fully work for all mods, since we\n" +
                        "cannot fully emulate what ComputerCraft offers to the mods'\n" +
                        "callbacks. Meaning when used on untested blocks this can\n" +
                        "very much crash or deadlock your game.").
                getBoolean(computerCraftWrapEverything);

        config.save();
    }

    @Mod.EventHandler
    public void init(final FMLInitializationEvent e) {
        Registry.add(new HandlerAppEng());
        Registry.add(new HandlerAtomicScience());
        Registry.add(new HandlerBuildCraft());
        Registry.add(new HandlerEnderStorage());
        Registry.add(new HandlerIndustrialCraft2());
        Registry.add(new HandlerMekanism());
        Registry.add(new HandlerRedstoneInMotion());
        Registry.add(new HandlerThermalExpansion());
        Registry.add(new HandlerVanilla());

        // Register the general IPeripheral driver last, if at all, to avoid it
        // being used rather than other more concrete implementations, such as
        // is the case in the Redstone in Motion driver (replaces 'move').
        Registry.add(new HandlerComputerCraft());
    }
}
