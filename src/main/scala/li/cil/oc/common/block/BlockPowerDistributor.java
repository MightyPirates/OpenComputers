package li.cil.oc.common.block;

import li.cil.oc.common.tileentity.TileEntityPowerDistributor;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

public final class BlockPowerDistributor extends AbstractBlock {
    @Nullable
    @Override
    protected Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityPowerDistributor.class;
    }
}

