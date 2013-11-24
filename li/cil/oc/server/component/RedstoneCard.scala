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

  @LuaCallback(value = "getBundledInput", direct = true)
  def getBundledInput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    val color = checkColor(args, 1)
    result(owner.bundledInput(side, color))
  }

  @LuaCallback(value = "getBundledOutput", direct = true)
  def getBundledOutput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    val color = checkColor(args, 1)
    result(owner.bundledOutput(side, color))
  }

  @LuaCallback("setBundledOutput")
  def setBundledOutput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    val color = checkColor(args, 1)
    val value = args.checkInteger(2)
    owner.bundledOutput(side, color, value)
    result(owner.bundledOutput(side, color))
  }

  // ----------------------------------------------------------------------- //

  private def checkSide(args: Arguments, index: Int) = {
    val side = args.checkInteger(index)
    if (side < 0 || side > 5)
      throw new IllegalArgumentException("invalid side")
    owner.toGlobal(ForgeDirection.getOrientation(side))
  }

  private def checkColor(args: Arguments, index: Int): Int = {
    val color = args.checkInteger(index)
    if (color < 0 || color > 15)
      throw new IllegalArgumentException("invalid color")
    color
  }
}
