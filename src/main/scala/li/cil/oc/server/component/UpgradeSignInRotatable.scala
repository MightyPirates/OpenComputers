package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.tileentity.Rotatable

class UpgradeSignInRotatable(val host: EnvironmentHost with Rotatable) extends UpgradeSign {
  override val getNode = Network.newNode(this, Visibility.NETWORK).
    withComponent("sign", Visibility.NEIGHBORS).
    withConnector().
    create()

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():string -- Get the text on the sign in front of the host.""")
  def getValue(context: Context, args: Arguments): Array[AnyRef] = super.getValue(findSign(host.getFacing))

  @Callback(doc = """function(value:string):string -- Set the text on the sign in front of the host.""")
  def setValue(context: Context, args: Arguments): Array[AnyRef] = super.setValue(findSign(host.getFacing), args.checkString(0))
}
