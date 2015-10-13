package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.internal.Rotatable
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._

class UpgradeSignInRotatable(val host: EnvironmentHost with Rotatable) extends UpgradeSign {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("sign", Visibility.Neighbors).
    withConnector().
    create()

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():string -- Get the text on the sign in front of the host.""")
  def getValue(context: Context, args: Arguments): Array[AnyRef] = super.getValue(findSign(host.facing))

  @Callback(doc = """function(value:string):string -- Set the text on the sign in front of the host.""")
  def setValue(context: Context, args: Arguments): Array[AnyRef] = super.setValue(findSign(host.facing), args.checkString(0))
}
