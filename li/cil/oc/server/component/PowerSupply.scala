package li.cil.oc.server.component

import li.cil.oc.api.network.{Context, Arguments, LuaCallback, Visibility}
import li.cil.oc.{Config, api}

class PowerSupply extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("psu").
    withConnector(Config.bufferPowerSupply).
    create()

  override def update() {
    super.update()
    node.changeBuffer(-Config.powerSupplyCost)
  }

  @LuaCallback(value = "localBufferSize", direct = true)
  def bufferSize(context: Context, args: Arguments): Array[AnyRef] = result(node.localBufferSize)

  @LuaCallback(value = "localBuffer", direct = true)
  def buffer(context: Context, args: Arguments): Array[AnyRef] = result(node.localBuffer)
}
