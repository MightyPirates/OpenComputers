package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.network.{Context, Arguments, LuaCallback, Visibility}

class PowerSupply extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("psu").
    withConnector(16).
    create()

  override def update() {
    super.update()
    node.changeBuffer(1)
  }

  @LuaCallback(value = "bufferSize", asynchronous = true)
  def bufferSize(context: Context, args: Arguments): Array[Object] = result(node.bufferSize)

  @LuaCallback(value = "buffer", asynchronous = true)
  def buffer(context: Context, args: Arguments): Array[Object] = result(node.buffer)
}
