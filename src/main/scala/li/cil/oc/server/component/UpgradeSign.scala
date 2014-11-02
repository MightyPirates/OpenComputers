package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.internal.Robot
import li.cil.oc.api.internal.Rotatable
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.integration.vanilla.DriverSign
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.tileentity.TileEntitySign

class UpgradeSign(val host: EnvironmentHost with Rotatable) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("sign", Visibility.Neighbors).
    withConnector().
    create()

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():string -- Get the text on the sign in front of the robot.""")
  def getValue(context: Context, args: Arguments): Array[AnyRef] = {
    findSign match {
      case Some(sign) => result(sign.signText.mkString("\n"))
      case _ => result(Unit, "no sign")
    }
  }

  @Callback(doc = """function(value:string):string -- Set the text on the sign in front of the robot.""")
  def setValue(context: Context, args: Arguments): Array[AnyRef] = {
    val text = args.checkString(0).lines.padTo(4, "").map(line => if (line.length > 15) line.substring(0, 15) else line)
    findSign match {
      case Some(sign) =>
        val player = host match {
          case robot: Robot => Option(robot.player)
          case _ => None
        }
        if (!DriverSign.canChangeSign(player.orNull, sign)) {
          return result(Unit, "not allowed")
        }

        text.copyToArray(sign.signText)
        host.world.markBlockForUpdate(sign.xCoord, sign.yCoord, sign.zCoord)
        result(sign.signText.mkString("\n"))
      case _ => result(Unit, "no sign")
    }
  }

  private def findSign = {
    val hostPos = BlockPosition(host)
    host.world.getTileEntity(hostPos) match {
      case sign: TileEntitySign => Option(sign)
      case _ => host.world.getTileEntity(hostPos.offset(host.facing)) match {
        case sign: TileEntitySign => Option(sign)
        case _ => None
      }
    }
  }
}
