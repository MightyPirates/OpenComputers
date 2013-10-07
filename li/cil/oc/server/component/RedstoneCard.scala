package li.cil.oc.server.component

import li.cil.oc.api.network.{Visibility, Message, Node}
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.ForgeDirection

class RedstoneCard extends Node {

  override def name = "redstone"

  override def visibility = Visibility.Neighbors

  override def receive(message: Message) = {
    super.receive(message)
    message.data match {
      case Array(target: Array[Byte], side: Double) if message.name == "redstone.input" =>
        input(new String(target, "UTF-8"), side.toInt)
      case Array(target: Array[Byte], side: Double) if message.name == "redstone.output" =>
        output(new String(target, "UTF-8"), side.toInt)
      case Array(target: Array[Byte], side: Double, value: Double) if message.name == "redstone.output=" =>
        output(new String(target, "UTF-8"), side.toInt, value.toInt); None
      case _ => None // Ignore.
    }
  }

  private def tryGet(target: String) = network.fold(None: Option[Node])(_.node(target))

  private def input(target: String, side: Int) = if (side >= 0 && side < 6) tryGet(target) match {
    case Some(r: RedstoneEnabled) => result(r.input(ForgeDirection.getOrientation(side)))
    case Some(t: TileEntity) =>
      val face = ForgeDirection.getOrientation(side.toInt)
      val power = t.worldObj.isBlockProvidingPowerTo(
        t.xCoord + face.offsetX, t.yCoord + face.offsetY, t.zCoord + face.offsetZ, face.getOpposite.ordinal)
      result(power)
    case _ => None // Can't work with this node.
  } else None

  private def output(target: String, side: Int) = if (side >= 0 && side < 6) tryGet(target) match {
    case Some(r: RedstoneEnabled) => result(r.output(ForgeDirection.getOrientation(side)))
    case Some(t: TileEntity) =>
      val power = t.worldObj.isBlockProvidingPowerTo(t.xCoord, t.yCoord, t.zCoord, side.toInt)
      result(power)
    case _ => None // Can't work with this node.
  } else None

  private def output(target: String, side: Int, value: Int) = if (side >= 0 && side < 6) tryGet(target) match {
    case Some(r: RedstoneEnabled) => r.output(ForgeDirection.getOrientation(side)) = value
    case _ => // Can't work with this node.
  }
}
