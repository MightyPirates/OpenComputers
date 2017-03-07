package li.cil.oc.common.block;

import li.cil.oc.common.tileentity.TileEntityRedstoneIO;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

public final class BlockRedstoneIO extends AbstractBlock {
    @Nullable
    @Override
    protected Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityRedstoneIO.class;
    }
}
