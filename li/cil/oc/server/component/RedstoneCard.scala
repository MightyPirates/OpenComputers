package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.ForgeDirection

class RedstoneCard extends ItemComponent {
  override def name = "redstone"

  override protected def receiveFromNeighbor(network: Network, message: Message) = message.data match {
    case Array(target: Double, side: Double) if message.name == "redstone.input" =>
      input(target.toInt, side.toInt)
    case Array(target: Double, side: Double) if message.name == "redstone.output" =>
      output(target.toInt, side.toInt)
    case Array(target: Double, side: Double, value: Double) if message.name == "redstone.output=" =>
      output(target.toInt, side.toInt, value.toInt); None
    case _ => None // Ignore.
  }

  private def tryGet(target: Int) = network.fold(None: Option[Node])(_.node(target))

  private def input(target: Int, side: Int) = if (side >= 0 && side < 6) tryGet(target) match {
    case Some(r: RedstoneEnabled) => Some(Array(r.input(ForgeDirection.getOrientation(side)).asInstanceOf[Any]))
    case Some(t: TileEntity) =>
      val face = ForgeDirection.getOrientation(side.toInt)
      val power = t.worldObj.isBlockProvidingPowerTo(
        t.xCoord + face.offsetX, t.yCoord + face.offsetY, t.zCoord + face.offsetZ, face.getOpposite.ordinal)
      Some(Array(power.asInstanceOf[Any]))
    case _ => None // Can't work with this node.
  } else None

  private def output(target: Int, side: Int) = if (side >= 0 && side < 6) tryGet(target) match {
    case Some(r: RedstoneEnabled) => Some(Array(r.output(ForgeDirection.getOrientation(side)).asInstanceOf[Any]))
    case Some(t: TileEntity) =>
      val power = t.worldObj.isBlockProvidingPowerTo(t.xCoord, t.yCoord, t.zCoord, side.toInt)
      Some(Array(power.asInstanceOf[Any]))
    case _ => None // Can't work with this node.
  } else None

  private def output(target: Int, side: Int, value: Int) = if (side >= 0 && side < 6) tryGet(target) match {
    case Some(r: RedstoneEnabled) => r.output(ForgeDirection.getOrientation(side)) = value
    case _ => // Can't work with this node.
  }
}
