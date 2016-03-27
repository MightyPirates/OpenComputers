package li.cil.oc.integration.mcmp

import java.util
import java.util.Collections

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.common.EventHandler
import li.cil.oc.common.tileentity.Cable
import li.cil.oc.server.PacketSender
import mcmultipart.multipart.IMultipart
import mcmultipart.multipart.IMultipartContainer
import mcmultipart.multipart.IPartConverter.IPartConverter2
import mcmultipart.multipart.IPartConverter.IReversePartConverter
import net.minecraft.block.Block
import net.minecraft.util.BlockPos
import net.minecraft.world.IBlockAccess

object PartConverter extends IPartConverter2 with IReversePartConverter {
  final lazy val CableBlock = api.Items.get(Constants.BlockName.Cable).block()

  override def getConvertableBlocks: util.Collection[Block] = Collections.singletonList(CableBlock)

  override def convertBlock(world: IBlockAccess, pos: BlockPos, simulated: Boolean): util.Collection[_ <: IMultipart] = {
    world.getTileEntity(pos) match {
      case tileEntity: Cable =>
        val part = new PartCable()
        part.setColor(tileEntity.getColor)
        Collections.singletonList(part)
      case _ => Collections.emptyList()
    }
  }

  override def convertToBlock(container: IMultipartContainer): Boolean = {
    val parts = container.getParts
    (parts.size() == 1) && (parts.iterator().next() match {
      case part: PartCable =>
        // TODO Create temporary node bridging hole left by removing multipart, remove after tile entity creation?
        val color = part.getColor
        val world = container.getWorldIn
        val pos = container.getPosIn
        world.setBlockToAir(pos)
        world.setBlockState(pos, CableBlock.getDefaultState)
        world.getTileEntity(pos) match {
          case tileEntity: Cable =>
            tileEntity.setColor(color)
            EventHandler.scheduleServer(() => PacketSender.sendColorChange(tileEntity)) // HACKS!
          case _ =>
        }
        true
      case _ =>
        false
    })
  }
}
