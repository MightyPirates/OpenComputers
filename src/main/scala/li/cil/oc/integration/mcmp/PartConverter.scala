package li.cil.oc.integration.mcmp

import java.util
import java.util.Collections

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.common.EventHandler
import li.cil.oc.common.tileentity.Cable
import li.cil.oc.common.tileentity.Print
import li.cil.oc.server.PacketSender
import mcmultipart.multipart.IMultipart
import mcmultipart.multipart.IMultipartContainer
import mcmultipart.multipart.IPartConverter.IPartConverter2
import mcmultipart.multipart.IPartConverter.IReversePartConverter
import net.minecraft.block.Block
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.BlockPos
import net.minecraft.world.IBlockAccess

import scala.collection.convert.WrapAsScala._

object PartConverter extends IPartConverter2 with IReversePartConverter {
  final lazy val CableBlock = api.Items.get(Constants.BlockName.Cable).block()
  final lazy val PrintBlock = api.Items.get(Constants.BlockName.Print).block()

  override def getConvertableBlocks: util.Collection[Block] = util.Arrays.asList(CableBlock, PrintBlock)

  override def convertBlock(world: IBlockAccess, pos: BlockPos, simulated: Boolean): util.Collection[_ <: IMultipart] = {
    world.getTileEntity(pos) match {
      case tileEntity: Cable =>
        val part = new PartCable()
        part.setColor(tileEntity.getColor)
        if (!simulated && tileEntity.node != null && part.node != null) {
          // Immediately connect node to avoid short disconnect.
          for (node <- tileEntity.node.neighbors) {
            node.connect(part.node)
          }
        }
        Collections.singletonList(part)
      case tileEntity: Print =>
        val part = new PartPrint()
        val nbt = new NBTTagCompound()
        tileEntity.writeToNBTForServer(nbt)
        part.wrapped.readFromNBTForServer(nbt)
        Collections.singletonList(part)
      case _ => Collections.emptyList()
    }
  }

  override def convertToBlock(container: IMultipartContainer): Boolean = {
    val world = container.getWorldIn
    val pos = container.getPosIn
    val parts = container.getParts
    (parts.size() == 1) && (parts.iterator().next() match {
      case part: PartCable =>
        val color = part.getColor
        world.setBlockToAir(pos)
        world.setBlockState(pos, CableBlock.getDefaultState)
        world.getTileEntity(pos) match {
          case tileEntity: Cable =>
            tileEntity.setColor(color)
            EventHandler.scheduleServer(() => PacketSender.sendColorChange(tileEntity)) // HACKS!
          case _ =>
        }
        // Immediately connect node to avoid short disconnect.
        api.Network.joinOrCreateNetwork(world, pos)
        // Required to dispose the old node because invalidate isn't forwarded to parts.
        // Can't use removePart because if the list is empty when this returns, MCMP
        // will tell MC that the block was removed so MC will set it to air.
        part.wrapped.invalidate()
        true
      case part: PartPrint =>
        val nbt = new NBTTagCompound()
        part.wrapped.writeToNBTForServer(nbt)
        world.setBlockToAir(pos)
        world.setBlockState(pos, PrintBlock.getDefaultState)
        world.getTileEntity(pos) match {
          case tileEntity: Print =>
            tileEntity.readFromNBTForServer(nbt)
          case _ =>
        }
        true
      case _ =>
        false
    })
  }
}
