package li.cil.oc.common.block;

import li.cil.oc.common.tileentity.TileEntityNetworkBridge;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

public class BlockNetworkBridge extends AbstractBlock {
    @Nullable
    @Override
    protected Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityNetworkBridge.class;
    }
}
