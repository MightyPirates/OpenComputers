package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.world.IBlockReader

class CarpetedCapacitor extends Capacitor {
  override def newBlockEntity(world: IBlockReader) = new tileentity.CarpetedCapacitor(tileentity.TileEntityTypes.CARPETED_CAPACITOR)
}
