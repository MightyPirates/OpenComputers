package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedArguments._

class UpgradeSignInAdapter(val host: EnvironmentHost) extends UpgradeSign {
  override val getNode = Network.newNode(this, Visibility.NETWORK).
    withComponent("sign", Visibility.NETWORK).
    withConnector().
    create()

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function(side:number):string -- Get the text on the sign on the specified side of the adapter.""")
  def getValue(context: Context, args: Arguments): Array[AnyRef] = super.getValue(findSign(args.checkSideAny(0)))

  @Callback(doc = """function(side:number, value:string):string -- Set the text on the sign on the specified side of the adapter.""")
  def setValue(context: Context, args: Arguments): Array[AnyRef] = super.setValue(findSign(args.checkSideAny(0)), args.checkString(1))
}
