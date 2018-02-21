package li.cil.oc.common.block

import java.util

import li.cil.oc.common.capabilities.Capabilities
import li.cil.oc.common.tileentity
import li.cil.oc.util.Color
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.{Entity, EntityLivingBase}
import net.minecraft.item.{EnumDyeColor, ItemStack}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.property.ExtendedBlockState
import net.minecraftforge.common.property.IExtendedBlockState

import scala.reflect.ClassTag

class Cable(protected implicit val tileTag: ClassTag[tileentity.Cable]) extends SimpleBlock with traits.CustomDrops[tileentity.Cable] {
  // For Immibis Microblock support.
  val ImmibisMicroblocks_TransformableBlockMarker = null

  // For FMP part coloring.
  var colorMultiplierOverride: Option[Int] = None

  // ----------------------------------------------------------------------- //

  override def createBlockState() = new ExtendedBlockState(this, Array.empty, Array(property.PropertyTile.Tile))

  override def getExtendedState(state: IBlockState, world: IBlockAccess, pos: BlockPos): IBlockState =
    (state, world.getTileEntity(pos)) match {
      case (extendedState: IExtendedBlockState, cable: tileentity.Cable) =>
        extendedState.withProperty(property.PropertyTile.Tile, cable)
      case _ => state
    }

  // ----------------------------------------------------------------------- //

  override def isOpaqueCube(state: IBlockState): Boolean = false

  override def isFullCube(state: IBlockState): Boolean = false

  override def shouldSideBeRendered(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing) = true

  override def isSideSolid(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing) = false

  // ----------------------------------------------------------------------- //

  override def getPickBlock(state: IBlockState, target: RayTraceResult, world: World, pos: BlockPos, player: EntityPlayer) =
    world.getTileEntity(pos) match {
      case t: tileentity.Cable => t.createItemStack()
      case _ => createItemStack()
    }

  override def getBoundingBox(state: IBlockState, world: IBlockAccess, pos: BlockPos): AxisAlignedBB = Cable.bounds(world, pos)

  override def addCollisionBoxToList(state : IBlockState, world : World, pos : BlockPos, entityBox : AxisAlignedBB, boxes : util.List[AxisAlignedBB], entity : Entity) = {
    Cable.parts(world, pos, entityBox, boxes)
  }

  // ----------------------------------------------------------------------- //

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Cable()

  // ----------------------------------------------------------------------- //

  override def neighborChanged(state: IBlockState, world: World, pos: BlockPos, neighborBlock: Block, sourcePos: BlockPos) {
    world.notifyBlockUpdate(pos, state, state, 3)
    super.neighborChanged(state, world, pos, neighborBlock, sourcePos)
  }

  override protected def doCustomInit(tileEntity: tileentity.Cable, player: EntityLivingBase, stack: ItemStack): Unit = {
    super.doCustomInit(tileEntity, player, stack)
    if (!tileEntity.world.isRemote) {
      tileEntity.fromItemStack(stack)
    }
  }

  override protected def doCustomDrops(tileEntity: tileentity.Cable, player: EntityPlayer, willHarvest: Boolean): Unit = {
    super.doCustomDrops(tileEntity, player, willHarvest)
    if (!player.capabilities.isCreativeMode) {
      Block.spawnAsEntity(tileEntity.world, tileEntity.getPos, tileEntity.createItemStack())
    }
  }
}

object Cable {
  private final val MIN = 0.375
  private final val MAX = 1 - MIN

  final val DefaultBounds: AxisAlignedBB = new AxisAlignedBB(MIN, MIN, MIN, MAX, MAX, MAX)

  final val CachedParts: Array[AxisAlignedBB] = Array(
    new AxisAlignedBB( MIN, 0, MIN, MAX, MIN, MAX ), // Down
    new AxisAlignedBB( MIN, MAX, MIN, MAX, 1, MAX ), // Up
    new AxisAlignedBB( MIN, MIN, 0, MAX, MAX, MIN ), // North
    new AxisAlignedBB( MIN, MIN, MAX, MAX, MAX, 1 ), // South
    new AxisAlignedBB( 0, MIN, MIN, MIN, MAX, MAX ), // West
    new AxisAlignedBB( MAX, MIN, MIN, 1, MAX, MAX )) // East

  final val CachedBounds = {
    // 6 directions = 6 bits = 11111111b >> 2 = 0xFF >> 2
    (0 to 0xFF >> 2).map(mask => {
      EnumFacing.VALUES.foldLeft(DefaultBounds)((bound, side) => {
        if (((1 << side.getIndex) & mask) != 0) bound.union(CachedParts(side.ordinal()))
        else bound
      })
    }).toArray
  }

  def mask(side: EnumFacing, value: Int = 0) = value | (1 << side.getIndex)

  def neighbors(world: IBlockAccess, pos: BlockPos) = {
    var result = 0
    val tileEntity = world.getTileEntity(pos)
    for (side <- EnumFacing.values) {
      val tpos = pos.offset(side)
      val hasNode = hasNetworkNode(tileEntity, side)
      if (hasNode && (world match {
        case world: World => world.isBlockLoaded(tpos)
        case _ => !world.isAirBlock(tpos)
      })) {
        val neighborTileEntity = world.getTileEntity(tpos)
        if (neighborTileEntity != null && neighborTileEntity.getWorld != null) {
          val neighborHasNode = hasNetworkNode(neighborTileEntity, side.getOpposite)
          val canConnectColor = canConnectBasedOnColor(tileEntity, neighborTileEntity)
          val canConnectIM = canConnectFromSideIM(tileEntity, side) && canConnectFromSideIM(neighborTileEntity, side.getOpposite)
          if (neighborHasNode && canConnectColor && canConnectIM) {
            result = mask(side, result)
          }
        }
      }
    }
    result
  }

  def bounds(world: IBlockAccess, pos: BlockPos) = Cable.CachedBounds(Cable.neighbors(world, pos))

  def parts(world: IBlockAccess, pos: BlockPos, entityBox : AxisAlignedBB, boxes : util.List[AxisAlignedBB]) = {
    val center = Cable.DefaultBounds.offset(pos)
    if (entityBox.intersectsWith(center)) boxes.add(center)

    val mask = Cable.neighbors(world, pos)
    for (side <- EnumFacing.VALUES) {
      if(((1 << side.getIndex) & mask) != 0) {
        val part = Cable.CachedParts(side.ordinal()).offset(pos)
        if (entityBox.intersectsWith(part)) boxes.add(part)
      }
    }
  }

  private def hasNetworkNode(tileEntity: TileEntity, side: EnumFacing): Boolean = {
    if (tileEntity != null) {
      if (tileEntity.isInstanceOf[tileentity.RobotProxy]) return false

      if (tileEntity.hasCapability(Capabilities.SidedEnvironmentCapability, side)) {
        val host = tileEntity.getCapability(Capabilities.SidedEnvironmentCapability, side)
        if (host != null) {
          return if (tileEntity.getWorld.isRemote) host.canConnect(side) else host.sidedNode(side) != null
        }
      }

      if (tileEntity.hasCapability(Capabilities.EnvironmentCapability, side)) {
        val host = tileEntity.getCapability(Capabilities.EnvironmentCapability, side)
        if (host != null) return true
      }
    }

    false
  }

  private def getConnectionColor(tileEntity: TileEntity): Int = {
    if (tileEntity != null) {
      if (tileEntity.hasCapability(Capabilities.ColoredCapability, null)) {
        val colored = tileEntity.getCapability(Capabilities.ColoredCapability, null)
        if (colored != null && colored.controlsConnectivity) return colored.getColor
      }
    }

    Color.rgbValues(EnumDyeColor.SILVER)
  }

  private def canConnectBasedOnColor(te1: TileEntity, te2: TileEntity) = {
    val (c1, c2) = (getConnectionColor(te1), getConnectionColor(te2))
    c1 == c2 || c1 == Color.rgbValues(EnumDyeColor.SILVER) || c2 == Color.rgbValues(EnumDyeColor.SILVER)
  }

  private def canConnectFromSideIM(tileEntity: TileEntity, side: EnumFacing) =
    tileEntity match {
      case im: tileentity.traits.ImmibisMicroblock => im.ImmibisMicroblocks_isSideOpen(side.ordinal)
      case _ => true
    }
}
