package li.cil.oc.api;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 * Item stacks for all blocks OpenComputers defines.
 * </p>
 * The underlying items of those are all of type {@link ItemBlock}, so you can
 * use that to get the block ID if required.
 */
public final class Blocks {
    public static ItemStack
            AccessPoint,
            Adapter,
            Cable,
            Capacitor,
            Charger,
            CaseTier1,
            CaseTier2,
            CaseTier3,
            DiskDrive,
            Keyboard,
            HologramProjector,
            PowerConverter,
            PowerDistributor,
            RedstoneIO,
            Robot,
            ScreenTier1,
            ScreenTier2,
            ScreenTier3,
            ServerRack,
            Switch;

    private Blocks() {
    }
}
