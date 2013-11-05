package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.network.{LuaCallback, Context, Arguments, Visibility}
import net.minecraftforge.common.ForgeDirection

class RedstoneCard extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Neighbors).
    withComponent("redstone").
    create()

  @LuaCallback(value = "getInput", direct = true)
  def getInput(context: Context, args: Arguments): Array[Object] = {
    val side = args.checkInteger(0)
    node.network.node(context.address).host match {
      case redstone: Redstone =>
        result(redstone.input(ForgeDirection.getOrientation(side)))
      case _ => result(false)
    }
  }

  @LuaCallback(value = "getOutput", direct = true)
  def getOutput(context: Context, args: Arguments): Array[Object] = {
    val side = args.checkInteger(0)
    node.network.node(context.address).host match {
      case redstone: Redstone =>
        result(redstone.output(ForgeDirection.getOrientation(side)))
      case _ => result(false)
    }
  }

  @LuaCallback("setOutput")
  def setOutput(context: Context, args: Arguments): Array[Object] = {
    val side = args.checkInteger(0)
    val value = args.checkInteger(1)
    node.network.node(context.address).host match {
      case redstone: Redstone =>
        redstone.output(ForgeDirection.getOrientation(side), value)
        result(redstone.output(ForgeDirection.getOrientation(side)))
      case _ => result(false)
    }
  }
}
