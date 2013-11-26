package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.common.tileentity.Redstone
import net.minecraftforge.common.ForgeDirection

class RedstoneCard(val owner: Redstone) extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Neighbors).
    withComponent("redstone").
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
