package li.cil.oc.common.block;

import li.cil.oc.common.tileentity.TileEntityCharger;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

public final class BlockCharger extends AbstractBlock {
    @Nullable
    @Override
    protected Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityCharger.class;
    }
}
