package li.cil.occ;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import li.cil.occ.mods.Registry;
import li.cil.occ.mods.appeng.ModAppEng;
import li.cil.occ.mods.buildcraft.ModBuildCraft;
import li.cil.occ.mods.computercraft.ModComputerCraft;
import li.cil.occ.mods.enderstorage.ModEnderStorage;
import li.cil.occ.mods.forestry.ModForestry;
import li.cil.occ.mods.ic2.ModIndustrialCraft2;
import li.cil.occ.mods.mystcraft.ModMystcraft;
import li.cil.occ.mods.railcraft.ModRailcraft;
import li.cil.occ.mods.redstoneinmotion.ModRedstoneInMotion;
import li.cil.occ.mods.thaumcraft.ModThaumcraft;
import li.cil.occ.mods.thermalexpansion.ModThermalExpansion;
import li.cil.occ.mods.tmechworks.ModTMechworks;
import li.cil.occ.mods.vanilla.ModVanilla;
import net.minecraftforge.common.Configuration;

import java.util.logging.Logger;

@Mod(modid = OpenComponents.ID, name = OpenComponents.Name, version = OpenComponents.Version, useMetadata = true)
@NetworkMod
public class OpenComponents {
    public static final String ID = "OpenComponents";

    public static final String Name = "OpenComponents";

    public static final String Version = "@VERSION@";

    @Mod.Instance
    public static OpenComponents instance;

    public static final Logger Log = Logger.getLogger(ID);

    public static String[] modBlacklist = new String[]{
            ModThaumcraft.MOD_ID
    };

    public static String[] peripheralBlacklist = new String[]{
            "JAKJ.RedstoneInMotion.CarriageControllerEntity",
            "appeng.api.me.tiles.ICellProvider",
            "appeng.api.me.tiles.ICellProvider",
            "appeng.me.tile.TileController",
            "mods.railcraft.common.blocks.hidden.TileHidden",
            "net.minecraft.tileentity.TileEntityCommandBlock"
    };

    public static Boolean allowItemStackInspection = false;

    public static String fakePlayerName = "[OpenComponents]";

    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent e) {
        final Configuration config = new Configuration(e.getSuggestedConfigurationFile());

        modBlacklist = config.get("mods", "blacklist", modBlacklist, "" +
                "A list of mods (by mod id) for which support should NOT be\n" +
                "enabled. Use this to disable support for mods you feel should\n" +
                "not be controllable via computers (such as magic related mods,\n" +
                "which is why Thaumcraft is on this list by default.)").
                getStringList();

        peripheralBlacklist = config.get("computercraft", "blacklist", peripheralBlacklist, "" +
                "A list of tile entities by class name that should NOT be\n" +
                "accessible via the Adapter block. Add blocks here that can\n" +
                "lead to crashes or deadlocks (and report them, please!)").
                getStringList();

        allowItemStackInspection = config.get("vanilla", "allowItemStackInspection", false).
                getBoolean(false);

        fakePlayerName = config.get("general", "fakePlayerName", fakePlayerName).
                getString();

        config.save();
    }

    @Mod.EventHandler
    public void init(final FMLInitializationEvent e) {
        Registry.add(new ModAppEng());
        Registry.add(new ModBuildCraft());
        Registry.add(new ModEnderStorage());
        Registry.add(new ModForestry());
        Registry.add(new ModIndustrialCraft2());
        Registry.add(new ModMystcraft());
        Registry.add(new ModRailcraft());
        Registry.add(new ModRedstoneInMotion());
        Registry.add(new ModThaumcraft());
        Registry.add(new ModThermalExpansion());
        Registry.add(new ModTMechworks());
        Registry.add(new ModVanilla());

        // Register the general IPeripheral driver last, if at all, to avoid it
        // being used rather than other more concrete implementations, such as
        // is the case in the Redstone in Motion driver (replaces 'move').
        Registry.add(new ModComputerCraft());
    }
}
