package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.prefab
import net.minecraft.nbt.NBTTagCompound

trait RedstoneSignaller extends prefab.ManagedEnvironment {
  var wakeThreshold = 0

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function():number -- Get the current wake-up threshold.""")
  def getWakeThreshold(context: Context, args: Arguments): Array[AnyRef] = result(wakeThreshold)

  @Callback(doc = """function(threshold:number) -- Set the wake-up threshold.""")
  def setWakeThreshold(context: Context, args: Arguments): Array[AnyRef] = {
    wakeThreshold = args.checkInteger(0)
    null
  }

  // ----------------------------------------------------------------------- //

  def onRedstoneChanged(side: AnyRef, oldMaxValue: Int, newMaxValue: Int): Unit = {
    node.sendToReachable("computer.signal", "redstone_changed", side, int2Integer(oldMaxValue), int2Integer(newMaxValue))
    if (oldMaxValue < wakeThreshold && newMaxValue >= wakeThreshold) {
      node.sendToNeighbors("computer.start")
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound): Unit = {
    super.load(nbt)
    wakeThreshold = nbt.getInteger(Settings.namespace + "wakeThreshold")
  }

  override def save(nbt: NBTTagCompound): Unit = {
    super.save(nbt)
    nbt.setInteger(Settings.namespace + "wakeThreshold", wakeThreshold)
  }
}
