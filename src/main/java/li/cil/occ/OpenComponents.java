package li.cil.occ;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import li.cil.occ.mods.Registry;
import li.cil.occ.mods.appeng.ModAppEng;
import li.cil.occ.mods.atomicscience.ModAtomicScience;
import li.cil.occ.mods.buildcraft.ModBuildCraft;
import li.cil.occ.mods.computercraft.ModComputerCraft;
import li.cil.occ.mods.enderstorage.ModEnderStorage;
import li.cil.occ.mods.ic2.ModIndustrialCraft2;
import li.cil.occ.mods.mekanism.ModMekanism;
import li.cil.occ.mods.redstoneinmotion.ModRedstoneInMotion;
import li.cil.occ.mods.thermalexpansion.ModThermalExpansion;
import li.cil.occ.mods.vanilla.ModVanilla;
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
        Registry.add(new ModAppEng());
        Registry.add(new ModAtomicScience());
        Registry.add(new ModBuildCraft());
        Registry.add(new ModEnderStorage());
        Registry.add(new ModIndustrialCraft2());
        Registry.add(new ModMekanism());
        Registry.add(new ModRedstoneInMotion());
        Registry.add(new ModThermalExpansion());
        Registry.add(new ModVanilla());

        // Register the general IPeripheral driver last, if at all, to avoid it
        // being used rather than other more concrete implementations, such as
        // is the case in the Redstone in Motion driver (replaces 'move').
        Registry.add(new ModComputerCraft());
    }
}
