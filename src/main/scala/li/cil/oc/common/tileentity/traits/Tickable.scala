package li.cil.oc.common.tileentity.traits

import net.minecraft.tileentity.ITickableTileEntity

trait Tickable extends TileEntity with ITickableTileEntity {
  override def tick(): Unit = updateEntity()
}
