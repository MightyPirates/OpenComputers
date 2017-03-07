package li.cil.oc.common.block;

import li.cil.oc.common.tileentity.TileEntityGeolyzer;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

public final class BlockGeolyzer extends AbstractBlock {
    @Nullable
    @Override
    protected Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityGeolyzer.class;
    }
}
