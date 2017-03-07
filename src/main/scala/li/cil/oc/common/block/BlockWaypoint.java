package li.cil.oc.common.block;

import li.cil.oc.common.tileentity.TileEntityWaypoint;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

public final class BlockWaypoint extends AbstractBlock {
    @Nullable
    @Override
    protected Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityWaypoint.class;
    }
}
