package li.cil.oc.common.block;

import li.cil.oc.common.tileentity.TileEntityDiskDrive;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

public final class BlockDiskDrive extends AbstractBlock {
    @Nullable
    @Override
    protected Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityDiskDrive.class;
    }
}
