package li.cil.oc.common.tileentity

import li.cil.oc.api.network.Node
import li.cil.oc.server.component
import net.minecraft.nbt.CompoundNBT
import net.minecraft.tileentity.TileEntity

class MotionSensor extends TileEntity(null) with traits.Environment with traits.Tickable {
  val motionSensor = new component.MotionSensor(this)

  def node: Node = motionSensor.node

  override def updateEntity() {
    super.updateEntity()
    if (isServer) {
      motionSensor.update()
    }
  }

  override def loadForServer(nbt: CompoundNBT) {
    super.loadForServer(nbt)
    motionSensor.loadData(nbt)
  }

  override def saveForServer(nbt: CompoundNBT) {
    super.saveForServer(nbt)
    motionSensor.saveData(nbt)
  }
}
