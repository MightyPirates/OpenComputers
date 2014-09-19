package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network._
import li.cil.oc.api.tileentity.Rotatable
import li.cil.oc.common.component
import net.minecraft.tileentity.TileEntitySign

class UpgradeSign(val host: EnvironmentHost with Rotatable) extends component.ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("sign", Visibility.Neighbors).
    withConnector().
    create()

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():string -- Get the text on the sign in front of the robot.""")
  def getValue(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = host.facing
    host.world.getTileEntity(math.round(host.xPosition - 0.5).toInt + facing.offsetX, math.round(host.yPosition - 0.5).toInt + facing.offsetY, math.round(host.zPosition - 0.5).toInt + facing.offsetZ) match {
      case sign: TileEntitySign => result(sign.signText.mkString("\n"))
      case _ => result(Unit, "no sign")
    }
  }

  @Callback(doc = """function(value:string):string -- Set the text on the sign in front of the robot.""")
  def setValue(context: Context, args: Arguments): Array[AnyRef] = {
    val text = args.checkString(0).lines.padTo(4, "").map(line => if (line.length > 15) line.substring(0, 15) else line)
    val facing = host.facing
    val (sx, sy, sz) = (math.round(host.xPosition - 0.5).toInt + facing.offsetX, math.round(host.yPosition - 0.5).toInt + facing.offsetY, math.round(host.zPosition - 0.5).toInt + facing.offsetZ)
    host.world.getTileEntity(sx, sy, sz) match {
      case sign: TileEntitySign =>
        text.copyToArray(sign.signText)
        host.world.markBlockForUpdate(sx, sy, sz)
        result(sign.signText.mkString("\n"))
      case _ => result(Unit, "no sign")
    }
  }
}
