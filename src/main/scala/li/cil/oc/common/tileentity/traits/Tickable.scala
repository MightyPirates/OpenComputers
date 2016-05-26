package li.cil.oc.common.tileentity.traits

import net.minecraft.util.ITickable

trait Tickable extends ITickable {
  override def update(): Unit = updateEntity()

  def updateEntity(): Unit = {}
}
