package li.cil.oc.common.tileentity.traits

import net.minecraft.util.ITickable

trait Tickable extends TileEntity with ITickable {
  override def update(): Unit = updateEntity()
}
