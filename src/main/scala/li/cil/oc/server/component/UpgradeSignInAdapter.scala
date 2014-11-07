package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedArguments._
import net.minecraftforge.common.util.ForgeDirection

class UpgradeSignInAdapter(val host: EnvironmentHost) extends UpgradeSign {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("sign", Visibility.Network).
    withConnector().
    create()

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function(side:number):string -- Get the text on the sign on the specified side of the adapter.""")
  def getValue(context: Context, args: Arguments): Array[AnyRef] = super.getValue(findSign(args.checkSide(0, ForgeDirection.VALID_DIRECTIONS: _*)))

  @Callback(doc = """function(side:number, value:string):string -- Set the text on the sign on the specified side of the adapter.""")
  def setValue(context: Context, args: Arguments): Array[AnyRef] = super.setValue(findSign(args.checkSide(0, ForgeDirection.VALID_DIRECTIONS: _*)), args.checkString(1))
}
