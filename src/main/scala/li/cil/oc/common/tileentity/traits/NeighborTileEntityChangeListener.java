package li.cil.oc.common.tileentity.traits;

import net.minecraft.util.math.BlockPos;

public interface NeighborTileEntityChangeListener {
    void onTileEntityChanged(final BlockPos neighborPos);
}
