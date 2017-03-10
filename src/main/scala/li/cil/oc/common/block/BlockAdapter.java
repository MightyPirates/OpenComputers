package li.cil.oc.common.block;

import li.cil.oc.common.tileentity.TileEntityAdapter;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

public final class BlockAdapter extends AbstractBlock {
    @Nullable
    @Override
    protected Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityAdapter.class;
    }
}
