package li.cil.oc.common.tileentity

import li.cil.oc.api.network.Node
import li.cil.oc.server.component
import net.minecraft.nbt.NBTTagCompound

class MotionSensor extends traits.Environment {
  val motionSensor = new component.MotionSensor(this)

  def node: Node = motionSensor.node

  override def canUpdate = isServer

  override def updateEntity() {
    super.updateEntity()
    motionSensor.update()
  }

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    motionSensor.load(nbt)
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    motionSensor.save(nbt)
  }
}
