package li.cil.oc.common.block;

import li.cil.oc.common.tileentity.TileEntityCapacitor;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

public final class BlockCapacitor extends AbstractBlock {
    @Nullable
    @Override
    protected Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityCapacitor.class;
    }
}
