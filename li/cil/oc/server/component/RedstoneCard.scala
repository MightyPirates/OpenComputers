package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.network.environment.{Arguments, Context, LuaCallback}
import net.minecraftforge.common.ForgeDirection

class RedstoneCard extends ManagedComponent {
  val node = api.Network.createComponent(api.Network.createNode(this, "redstone", Visibility.Neighbors))

  @LuaCallback(value = "getInput", asynchronous = true)
  def getInput(context: Context, args: Arguments): Array[Object] = {
    val side = args.checkInteger(1)
    node.network.sendToAddress(node, context.address,
      "redstone.input", ForgeDirection.getOrientation(side))
  }

  @LuaCallback(value = "getOutput", asynchronous = true)
  def getOutput(context: Context, args: Arguments): Array[Object] = {
    val side = args.checkInteger(1)
    node.network.sendToAddress(node, context.address,
      "redstone.output", ForgeDirection.getOrientation(side))
  }

  @LuaCallback("setOutput")
  def setOutput(context: Context, args: Arguments): Array[Object] = {
    val side = args.checkInteger(1)
    val value = args.checkInteger(2)
    node.network.sendToAddress(node, context.address,
      "redstone.output=", ForgeDirection.getOrientation(side.toInt), Int.box(value))
  }
}
