package li.cil.oc.server.component

import li.cil.oc.api.driver.Container
import li.cil.oc.api.network._
import li.cil.oc.api.{Network, Rotatable}
import li.cil.oc.common.component
import net.minecraft.tileentity.TileEntitySign

class UpgradeSign(val owner: Container with Rotatable) extends component.ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("sign", Visibility.Neighbors).
    withConnector().
    create()

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():string -- Get the text on the sign in front of the robot.""")
  def getValue(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = owner.facing
    owner.world.getTileEntity(math.round(owner.xPosition - 0.5).toInt + facing.offsetX, math.round(owner.yPosition - 0.5).toInt + facing.offsetY, math.round(owner.zPosition - 0.5).toInt + facing.offsetZ) match {
      case sign: TileEntitySign => result(sign.signText.mkString("\n"))
      case _ => result(Unit, "no sign")
    }
  }

  @Callback(doc = """function(value:string):string -- Set the text on the sign in front of the robot.""")
  def setValue(context: Context, args: Arguments): Array[AnyRef] = {
    val text = args.checkString(0).lines.padTo(4, "").map(line => if (line.length > 15) line.substring(0, 15) else line)
    val facing = owner.facing
    val (sx, sy, sz) = (math.round(owner.xPosition - 0.5).toInt + facing.offsetX, math.round(owner.yPosition - 0.5).toInt + facing.offsetY, math.round(owner.zPosition - 0.5).toInt + facing.offsetZ)
    owner.world.getTileEntity(sx, sy, sz) match {
      case sign: TileEntitySign =>
        text.copyToArray(sign.signText)
        owner.world.markBlockForUpdate(sx, sy, sz)
        result(sign.signText.mkString("\n"))
      case _ => result(Unit, "no sign")
    }
  }
}
