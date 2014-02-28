package li.cil.occ;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import li.cil.occ.mods.Registry;
import li.cil.occ.mods.buildcraft.ModBuildCraft;
import li.cil.occ.mods.ic2.ModIndustrialCraft2;
import li.cil.occ.mods.tmechworks.ModTMechworks;
import li.cil.occ.mods.vanilla.ModVanilla;
import net.minecraftforge.common.config.Configuration;

import java.util.logging.Logger;

@Mod(modid = "OpenComponents", useMetadata = true)
public class OpenComponents {
    @Mod.Instance
    public static OpenComponents instance;

    public static final Logger Log = Logger.getLogger("OpenComponents");

    public static String[] peripheralBlacklist = new String[]{
            "JAKJ.RedstoneInMotion.CarriageControllerEntity"
    };

    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent e) {
        final Configuration config = new Configuration(e.getSuggestedConfigurationFile());

        peripheralBlacklist = config.get("computercraft", "blacklist", peripheralBlacklist, "" +
                "A list of tile entities by class name that should NOT be\n" +
                "accessible via the Adapter block. Add blocks here that can\n" +
                "lead to crashes or deadlocks (and report them, please!)").
                getStringList();

        config.save();
    }

    @Mod.EventHandler
    public void init(final FMLInitializationEvent e) {
        Registry.add(new ModBuildCraft());
        Registry.add(new ModIndustrialCraft2());
        Registry.add(new ModTMechworks());
        Registry.add(new ModVanilla());

        // Register the general IPeripheral driver last, if at all, to avoid it
        // being used rather than other more concrete implementations, such as
        // is the case in the Redstone in Motion driver (replaces 'move').
        //   Registry.add(new ModComputerCraft());
    }
}
