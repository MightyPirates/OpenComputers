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
import li.cil.oc.common.capabilities.Capabilities
import li.cil.oc.common.tileentity
import li.cil.oc.util.Color
import mcmultipart.capabilities.ISlottedCapabilityProvider
import mcmultipart.multipart._
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
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.property.ExtendedBlockState
import net.minecraftforge.common.property.IExtendedBlockState

import scala.collection.convert.WrapAsScala._

class PartCable extends Multipart with ISlottedPart with IOccludingPart with ISlottedCapabilityProvider with Environment with Colored {
  final val CableDefinition = api.Items.get(Constants.BlockName.Cable)
  final val CableBlock = CableDefinition.block()

  val wrapped = new tileentity.Cable()

  // ----------------------------------------------------------------------- //
  // Environment

  override def node = wrapped.node

  override def onMessage(message: Message) = wrapped.onMessage(message)

  override def onConnect(node: Node) = wrapped.onConnect(node)

  override def onDisconnect(node: Node) = wrapped.onDisconnect(node)

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
  // IOccludingPart

  override def addOcclusionBoxes(list: util.List[AxisAlignedBB]): Unit = list.add(Cable.DefaultBounds)

  // ----------------------------------------------------------------------- //
  // ISlottedCapabilityProvider

  override def hasCapability(capability: Capability[_], slot: PartSlot, facing: EnumFacing): Boolean = {
    capability == Capabilities.EnvironmentCapability && canConnect(facing)
  }

  override def getCapability[T](capability: Capability[T], slot: PartSlot, facing: EnumFacing): T = {
    if (capability == Capabilities.EnvironmentCapability && canConnect(facing)) this.asInstanceOf[T]
    else null.asInstanceOf[T]
  }

  override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = {
    (capability == Capabilities.ColoredCapability) || super.hasCapability(capability, facing)
  }

  override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = {
    if (capability == Capabilities.ColoredCapability) this.asInstanceOf[T]
    else super.getCapability(capability, facing)
  }

  private def canConnect(facing: EnumFacing) =
    !OcclusionHelper.isSlotOccluded(getContainer.getParts, this, PartSlot.getFaceSlot(facing)) &&
      OcclusionHelper.occlusionTest(getContainer.getParts, this, Cable.CachedBounds(Cable.mask(facing)))

  // ----------------------------------------------------------------------- //
  // IMultipart

  override def addSelectionBoxes(list: util.List[AxisAlignedBB]): Unit = {
    if (getWorld != null) {
      list.add(Cable.bounds(getWorld, getPos))
    }
  }

  override def addCollisionBoxes(mask: AxisAlignedBB, list: util.List[AxisAlignedBB], collidingEntity: Entity): Unit = {
    if (getWorld != null) {
      val bounds = Cable.bounds(getWorld, getPos)
      if (bounds.intersectsWith(mask)) list.add(bounds)
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

  override def onPartChanged(part: IMultipart): Unit = {
    super.onPartChanged(part)
    if (getWorld.isRemote) {
      markRenderUpdate()
    } else {
      // This is a bit meh... when a previously valid connection to a neighbor of
      // a multipart tile becomes invalid due to a new, occluding part, we can't
      // access the now occluded node anymore, so we can't just disconnect it from
      // it's neighbor. So instead we get the old list of neighbor nodes and kill
      // every connection for which we can't find a currently valid neighbor.
      val mask = Cable.neighbors(getWorld, getPos)
      val neighbors = EnumFacing.VALUES.filter(side => (mask & (1 << side.getIndex)) != 0).map(side => {
        val neighborPos = getPos.offset(side)
        val neighborTile = getWorld.getTileEntity(neighborPos)
        if (neighborTile == null || !canConnect(side)) null
        else if (neighborTile.hasCapability(Capabilities.SidedEnvironmentCapability, side.getOpposite)) {
          val host = neighborTile.getCapability(Capabilities.SidedEnvironmentCapability, side.getOpposite)
          if (host != null) host.sidedNode(side.getOpposite)
          else null
        }
        else if (neighborTile.hasCapability(Capabilities.EnvironmentCapability, side.getOpposite)) {
          val host = neighborTile.getCapability(Capabilities.EnvironmentCapability, side.getOpposite)
          if (host != null) host.node
          else null
        }
        else null
      }).filter(_ != null).toSet
      for (neighborNode <- node.neighbors.toSeq) {
        if (!neighbors.contains(neighborNode)) {
          node.disconnect(neighborNode)
        }
      }
      api.Network.joinOrCreateNetwork(getWorld, getPos)
    }
  }

  override def onAdded(): Unit = {
    super.onAdded()
    wrapped.setWorldObj(getWorld)
    wrapped.setPos(getPos)
    wrapped.validate()
  }

  override def onRemoved(): Unit = {
    super.onRemoved()
    wrapped.invalidate()
    wrapped.setWorldObj(null)
    wrapped.setPos(null)
  }

  override def onLoaded(): Unit = {
    super.onLoaded()
    wrapped.setWorldObj(getWorld)
    wrapped.setPos(getPos)
    wrapped.validate()
  }

  override def onUnloaded(): Unit = {
    super.onUnloaded()
    wrapped.onChunkUnload()
    wrapped.setWorldObj(null)
    wrapped.setPos(null)
  }

  override def onConverted(tile: TileEntity): Unit = {
    super.onConverted(tile)
    tile match {
      case cable: tileentity.Cable =>
        wrapped.setColor(cable.getColor)
      case _ =>
    }
    wrapped.setWorldObj(getWorld)
    wrapped.setPos(getPos)
    wrapped.validate()
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
