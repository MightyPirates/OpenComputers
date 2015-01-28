package li.cil.oc.server.component

import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.common.tileentity.traits.RedstoneAware
import net.minecraft.util.EnumFacing

trait RedstoneVanilla extends RedstoneSignaller {
  def redstone: EnvironmentHost with RedstoneAware

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function(side:number):number -- Get the redstone input on the specified side.""")
  def getInput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    result(redstone.input(side))
  }

  @Callback(direct = true, doc = """function(side:number):number -- Get the redstone output on the specified side.""")
  def getOutput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    result(redstone.output(side))
  }

  @Callback(doc = """function(side:number, value:number):number -- Set the redstone output on the specified side.""")
  def setOutput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    val value = args.checkInteger(1)
    redstone.output(side, value)
    context.pause(0.1)
    result(redstone.output(side))
  }

  // ----------------------------------------------------------------------- //

  override def onMessage(message: Message): Unit = {
    super.onMessage(message)
    if (message.name == "redstone.changed") message.data match {
      case Array(side: EnumFacing, oldMaxValue: Number, newMaxValue: Number) =>
        onRedstoneChanged(int2Integer(side.ordinal()), oldMaxValue.intValue(), newMaxValue.intValue())
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  protected def checkSide(args: Arguments, index: Int) = {
    val side = args.checkInteger(index)
    if (side < 0 || side > 5)
      throw new IllegalArgumentException("invalid side")
    redstone.toGlobal(EnumFacing.getFront(side))
  }
}
