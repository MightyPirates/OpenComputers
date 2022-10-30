package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.common.tileentity.traits.RedstoneChangedEventArgs
import net.minecraft.nbt.CompoundNBT

import scala.collection.mutable.ArrayBuffer

trait RedstoneSignaller extends AbstractManagedEnvironment {
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

  def onRedstoneChanged(args: RedstoneChangedEventArgs): Unit = {
    val side: AnyRef = if (args.side == null) "wireless" else Int.box(args.side.ordinal)
    val flatArgs = ArrayBuffer[Object]("redstone_changed", side, Int.box(args.oldValue), Int.box(args.newValue))
    if (args.color >= 0)
      flatArgs += Int.box(args.color)
    node.sendToReachable("computer.signal", flatArgs.toArray: _*)
    if (args.oldValue < wakeThreshold && args.newValue >= wakeThreshold) {
      if (wakeNeighborsOnly)
        node.sendToNeighbors("computer.start")
      else
        node.sendToReachable("computer.start")
    }
  }

  // ----------------------------------------------------------------------- //

  private final val WakeThresholdNbt = "wakeThreshold"

  override def loadData(nbt: CompoundNBT): Unit = {
    super.loadData(nbt)
    wakeThreshold = nbt.getInt(WakeThresholdNbt)
  }

  override def saveData(nbt: CompoundNBT): Unit = {
    super.saveData(nbt)
    nbt.putInt(WakeThresholdNbt, wakeThreshold)
  }
}
