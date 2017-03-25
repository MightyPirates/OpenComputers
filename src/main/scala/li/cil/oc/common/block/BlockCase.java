package li.cil.oc.common.block;

import li.cil.oc.common.tileentity.TileEntityCase;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

public final class BlockCase extends AbstractBlock {
    public final int tier;

    public BlockCase(final int tier) {
        this.tier = tier;
    }

    @Nullable
    @Override
    protected Class<? extends TileEntity> getTileEntityClass() {
        return TileEntityCase.class;
    }

// ----------------------------------------------------------------------- //

//  override def rarity(stack: ItemStack) = RarityUtils.fromTier(tier)
//
//  override protected def tooltipBody(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
//    tooltip.addAll(Tooltip.get(getClass.getSimpleName, slots))
//  }
//
//  private def slots = tier match {
//    case 0 => "2/1/1"
//    case 1 => "2/2/2"
//    case 2 | 3 => "3/2/3"
//    case _ => "0/0/0"
//  }
}
