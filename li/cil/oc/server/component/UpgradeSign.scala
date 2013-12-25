package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.util.RotationHelper
import net.minecraft.tileentity.{TileEntity => MCTileEntity, TileEntitySign}

class UpgradeSign(val owner: MCTileEntity) extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("sign", Visibility.Neighbors).
    withConnector().
    create()

  // ----------------------------------------------------------------------- //

  @LuaCallback("getValue")
  def read(context: RobotContext, args: Arguments): Array[AnyRef] = {
    val player = context.player()
    val facing = RotationHelper.fromYaw(player.rotationYaw)
    owner.getWorldObj.getBlockTileEntity(owner.xCoord + facing.offsetX, owner.yCoord + facing.offsetY, owner.zCoord + facing.offsetZ) match {
      case sign: TileEntitySign => result(sign.signText.mkString("\n"))
      case _ => result(Unit, "no sign")
    }
  }

  @LuaCallback("setValue")
  def write(context: RobotContext, args: Arguments): Array[AnyRef] = {
    val text = args.checkString(0).lines.padTo(4, "").map(line => if (line.size > 15) line.substring(0, 15) else line)
    val player = context.player()
    val facing = RotationHelper.fromYaw(player.rotationYaw)
    val (sx, sy, sz) = (owner.xCoord + facing.offsetX, owner.yCoord + facing.offsetY, owner.zCoord + facing.offsetZ)
    owner.getWorldObj.getBlockTileEntity(sx, sy, sz) match {
      case sign: TileEntitySign =>
        text.copyToArray(sign.signText)
        owner.getWorldObj.markBlockForUpdate(sx, sy, sz)
        result(sign.signText.mkString("\n"))
      case _ => result(Unit, "no sign")
    }
  }
}
