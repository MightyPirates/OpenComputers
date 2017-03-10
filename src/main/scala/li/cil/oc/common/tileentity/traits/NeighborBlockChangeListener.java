package li.cil.oc.common.tileentity.traits;

import net.minecraft.util.math.BlockPos;

public interface NeighborBlockChangeListener {
    void onBlockChanged(final BlockPos neighborPos);
}
