package li.cil.oc.integration.mcmp

import java.util

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.api.internal.Colored
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.common.block.Cable
import li.cil.oc.common.block.property
import li.cil.oc.common.tileentity
import li.cil.oc.util.Color
import mcmultipart.multipart.ISlottedPart
import mcmultipart.multipart.Multipart
import mcmultipart.multipart.PartSlot
import mcmultipart.raytrace.PartMOP
import net.minecraft.block.material.Material
import net.minecraft.block.state.BlockState
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.common.property.IExtendedBlockState
import net.minecraftforge.common.property.ExtendedBlockState

class PartCable extends Multipart with ISlottedPart with Environment with Colored {
  final val CableDefinition = api.Items.get(Constants.BlockName.Cable)
  final val CableBlock = CableDefinition.block()

  val wrapped = new tileentity.Cable()

  // ----------------------------------------------------------------------- //
  // Environment

  override def node(): Node = wrapped.node

  override def onMessage(message: Message): Unit = wrapped.onMessage(message)

  override def onConnect(node: Node): Unit = wrapped.onConnect(node)

  override def onDisconnect(node: Node): Unit = wrapped.onDisconnect(node)

  // ----------------------------------------------------------------------- //
  // Colored

  override def getColor = wrapped.getColor

  override def setColor(value: Int): Unit = {
    if (value != getColor) {
      wrapped.setColor(value)
      if (getWorld != null && !getWorld.isRemote) {
        sendUpdatePacket(true)
      }
    }
  }

  override def controlsConnectivity = wrapped.controlsConnectivity

  // ----------------------------------------------------------------------- //
  // ISlottedPart

  override def getSlotMask: util.EnumSet[PartSlot] = util.EnumSet.of(PartSlot.CENTER)

  // ----------------------------------------------------------------------- //
  // IMultipart

  override def addSelectionBoxes(list: util.List[AxisAlignedBB]): Unit = {
    if (getWorld != null) {
      list.add(Cable.bounds(getWorld, getPos))
    }
  }

  override def addCollisionBoxes(mask: AxisAlignedBB, list: util.List[AxisAlignedBB], collidingEntity: Entity): Unit = {
    if (getWorld != null) {
      list.add(Cable.bounds(getWorld, getPos)) //.offset(getPos.getX, getPos.getY, getPos.getZ))
    }
  }

  override def getPickBlock(player: EntityPlayer, hit: PartMOP): ItemStack = wrapped.createItemStack()

  override def getDrops: util.List[ItemStack] = util.Arrays.asList(wrapped.createItemStack())

  override def getHardness(hit: PartMOP): Float = CableBlock.getBlockHardness(getWorld, getPos)

  override def getMaterial: Material = CableBlock.getMaterial

  override def isToolEffective(toolType: String, level: Int): Boolean = CableBlock.isToolEffective(toolType, getWorld.getBlockState(getPos))

  // ----------------------------------------------------------------------- //

  override def getModelPath = MCMultiPart.CableMultipartLocation

  override def createBlockState(): BlockState = new ExtendedBlockState(CableBlock, Array.empty, Array(property.PropertyTile.Tile))

  override def getExtendedState(state: IBlockState): IBlockState =
    state match {
      case extendedState: IExtendedBlockState =>
        wrapped.setWorldObj(getWorld)
        wrapped.setPos(getPos)
        extendedState.withProperty(property.PropertyTile.Tile, wrapped)
      case _ => state
    }

  override def onAdded(): Unit = {
    super.onAdded()
    wrapped.validate()
    wrapped.setWorldObj(getWorld)
    wrapped.setPos(getPos)
  }

  override def onRemoved(): Unit = {
    super.onRemoved()
    wrapped.invalidate()
    wrapped.setWorldObj(null)
    wrapped.setPos(null)
  }

  override def onLoaded(): Unit = {
    super.onLoaded()
    wrapped.validate()
    wrapped.setWorldObj(getWorld)
    wrapped.setPos(getPos)
  }

  override def onUnloaded(): Unit = {
    super.onUnloaded()
    wrapped.invalidate()
    wrapped.setWorldObj(null)
    wrapped.setPos(null)
  }

  override def onConverted(tile: TileEntity): Unit = {
    super.onConverted(tile)
    val nbt = new NBTTagCompound()
    tile.writeToNBT(nbt)
    wrapped.readFromNBT(nbt)
  }

  // ----------------------------------------------------------------------- //

  override def onActivated(player: EntityPlayer, heldItem: ItemStack, hit: PartMOP): Boolean = {
    if (Color.isDye(heldItem)) {
      setColor(Color.rgbValues(Color.dyeColor(player.getHeldItem)))
      markDirty()
      if (!player.capabilities.isCreativeMode && wrapped.consumesDye) {
        player.getHeldItem.splitStack(1)
      }
      true
    }
    else super.onActivated(player, heldItem, hit)
  }

  // ----------------------------------------------------------------------- //

  override def writeToNBT(tag: NBTTagCompound): Unit = {
    super.writeToNBT(tag)
    wrapped.writeToNBT(tag)
  }

  override def readFromNBT(tag: NBTTagCompound): Unit = {
    super.readFromNBT(tag)
    wrapped.readFromNBT(tag)
  }

  override def writeUpdatePacket(buf: PacketBuffer): Unit = {
    super.writeUpdatePacket(buf)
    buf.writeInt(getColor)
  }

  override def readUpdatePacket(buf: PacketBuffer): Unit = {
    super.readUpdatePacket(buf)
    setColor(buf.readInt())
  }
}
