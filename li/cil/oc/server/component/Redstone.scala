package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.network._
import li.cil.oc.common.tileentity.RedstoneAware
import net.minecraftforge.common.ForgeDirection

class Redstone(val owner: RedstoneAware) extends ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("redstone", Visibility.Neighbors).
    create()

  // ----------------------------------------------------------------------- //

  @LuaCallback(value = "getInput", direct = true)
  def getInput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    result(owner.input(side))
  }

  @LuaCallback(value = "getOutput", direct = true)
  def getOutput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    result(owner.output(side))
  }

  @LuaCallback("setOutput")
  def setOutput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    val value = args.checkInteger(1)
    owner.output(side, value)
    result(owner.output(side))
  }

  // ----------------------------------------------------------------------- //

  protected def checkSide(args: Arguments, index: Int) = {
    val side = args.checkInteger(index)
    if (side < 0 || side > 5)
      throw new IllegalArgumentException("invalid side")
    owner.toGlobal(ForgeDirection.getOrientation(side))
  }
}
