package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import net.minecraft.nbt.NBTTagCompound

trait RedstoneSignaller extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("redstone", Visibility.Neighbors).
    create()

  var wakeThreshold = 0

  var wakeNeighborsOnly = true

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function():number -- Get the current wake-up threshold.""")
  def getWakeThreshold(context: Context, args: Arguments): Array[AnyRef] = result(wakeThreshold)

  @Callback(doc = """function(threshold:number):number -- Set the wake-up threshold.""")
  def setWakeThreshold(context: Context, args: Arguments): Array[AnyRef] = {
    val oldThreshold = wakeThreshold
    wakeThreshold = args.checkInteger(0)
    result(oldThreshold)
  }

  // ----------------------------------------------------------------------- //

  def onRedstoneChanged(side: AnyRef, oldMaxValue: Int, newMaxValue: Int): Unit = {
    node.sendToReachable("computer.signal", "redstone_changed", side, Int.box(oldMaxValue), Int.box(newMaxValue))
    if (oldMaxValue < wakeThreshold && newMaxValue >= wakeThreshold) {
      if (wakeNeighborsOnly)
        node.sendToNeighbors("computer.start")
      else
        node.sendToReachable("computer.start")
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound): Unit = {
    super.load(nbt)
    wakeThreshold = nbt.getInteger("wakeThreshold")
  }

  override def save(nbt: NBTTagCompound): Unit = {
    super.save(nbt)
    nbt.setInteger("wakeThreshold", wakeThreshold)
  }
}
