package li.cil.oc.server.component

import li.cil.oc.api.network._
import li.cil.oc.api.{Rotatable, Network}
import net.minecraft.tileentity.{TileEntity, TileEntitySign}

class UpgradeSign(val owner: TileEntity) extends ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("sign", Visibility.Neighbors).
    withConnector().
    create()

  // ----------------------------------------------------------------------- //

  @Callback
  def getValue(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = owner match {
      case rotatable: Rotatable => rotatable.facing
      case _ => throw new Exception("illegal state")
    }
    owner.getWorldObj.getBlockTileEntity(owner.xCoord + facing.offsetX, owner.yCoord + facing.offsetY, owner.zCoord + facing.offsetZ) match {
      case sign: TileEntitySign => result(sign.signText.mkString("\n"))
      case _ => result(Unit, "no sign")
    }
  }

  @Callback
  def setValue(context: Context, args: Arguments): Array[AnyRef] = {
    val text = args.checkString(0).lines.padTo(4, "").map(line => if (line.length > 15) line.substring(0, 15) else line)
    val facing = owner match {
      case rotatable: Rotatable => rotatable.facing
      case _ => throw new Exception("illegal state")
    }
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
