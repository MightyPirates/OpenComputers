package li.cil.oc.common.block

import java.util

import li.cil.oc.common.capabilities.Capabilities
import li.cil.oc.common.tileentity
import li.cil.oc.util.Color
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.{Entity, LivingEntity}
import net.minecraft.item.{DyeColor, ItemStack}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import net.minecraft.util.math.{AxisAlignedBB, BlockPos, RayTraceResult}
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.world.IBlockReader
import net.minecraft.world.World
import net.minecraftforge.common.extensions.IForgeBlock

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

class Cable(protected implicit val tileTag: ClassTag[tileentity.Cable]) extends SimpleBlock with IForgeBlock with traits.CustomDrops[tileentity.Cable] {
  // For Immibis Microblock support.
  val ImmibisMicroblocks_TransformableBlockMarker = null

  // For FMP part coloring.
  var colorMultiplierOverride: Option[Int] = None

  // ----------------------------------------------------------------------- //

  override def getPickBlock(state: BlockState, target: RayTraceResult, world: IBlockReader, pos: BlockPos, player: PlayerEntity) =
    world.getBlockEntity(pos) match {
      case t: tileentity.Cable => t.createItemStack()
      case _ => createItemStack()
    }

  override def getBoundingBox(state: BlockState, world: IBlockReader, pos: BlockPos): AxisAlignedBB = Cable.bounds(world, pos)

  // ----------------------------------------------------------------------- //

  override def newBlockEntity(world: IBlockReader) = new tileentity.Cable(tileentity.TileEntityTypes.CABLE)

  // ----------------------------------------------------------------------- //

  @Deprecated
  override def neighborChanged(state: BlockState, world: World, pos: BlockPos, neighborBlock: Block, sourcePos: BlockPos, b: Boolean) {
    world.sendBlockUpdated(pos, state, state, 3)
    super.neighborChanged(state, world, pos, neighborBlock, sourcePos, b)
  }

  override protected def doCustomInit(tileEntity: tileentity.Cable, player: LivingEntity, stack: ItemStack): Unit = {
    super.doCustomInit(tileEntity, player, stack)
    if (!tileEntity.world.isClientSide) {
      tileEntity.fromItemStack(stack)
    }
  }

  override protected def doCustomDrops(tileEntity: tileentity.Cable, player: PlayerEntity, willHarvest: Boolean): Unit = {
    super.doCustomDrops(tileEntity, player, willHarvest)
    if (!player.isCreative) {
      Block.popResource(tileEntity.world, tileEntity.getBlockPos, tileEntity.createItemStack())
    }
  }
}

object Cable {
  final val MIN = 0.375
  final val MAX = 1 - MIN

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
      Direction.values.foldLeft(DefaultBounds)((bound, side) => {
        if (((1 << side.get3DDataValue) & mask) != 0) bound.minmax(CachedParts(side.ordinal()))
        else bound
      })
    }).toArray
  }

  def mask(side: Direction, value: Int = 0) = value | (1 << side.get3DDataValue)

  def neighbors(world: IBlockReader, pos: BlockPos) = {
    var result = 0
    val tileEntity = world.getBlockEntity(pos)
    for (side <- Direction.values) {
      val tpos = pos.relative(side)
      val hasNode = hasNetworkNode(tileEntity, side)
      if (hasNode && (world match {
        case world: World => world.isLoaded(tpos)
        case _ => {
          val state = world.getBlockState(tpos)
          state.getBlock.isAir(state, world, tpos)
        }
      })) {
        val neighborTileEntity = world.getBlockEntity(tpos)
        if (neighborTileEntity != null && neighborTileEntity.getLevel != null) {
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

  def bounds(world: IBlockReader, pos: BlockPos) = Cable.CachedBounds(Cable.neighbors(world, pos))

  def parts(world: IBlockReader, pos: BlockPos, entityBox : AxisAlignedBB, boxes : util.List[AxisAlignedBB]) = {
    val center = Cable.DefaultBounds.move(pos)
    if (entityBox.intersects(center)) boxes.add(center)

    val mask = Cable.neighbors(world, pos)
    for (side <- Direction.values) {
      if(((1 << side.get3DDataValue) & mask) != 0) {
        val part = Cable.CachedParts(side.ordinal()).move(pos)
        if (entityBox.intersects(part)) boxes.add(part)
      }
    }
  }

  private def hasNetworkNode(tileEntity: TileEntity, side: Direction): Boolean = {
    if (tileEntity != null) {
      if (tileEntity.isInstanceOf[tileentity.RobotProxy]) return false

      if (tileEntity.getCapability(Capabilities.SidedEnvironmentCapability, side).isPresent) {
        val host = tileEntity.getCapability(Capabilities.SidedEnvironmentCapability, side).orElse(null)
        if (host != null) {
          return if (tileEntity.getLevel.isClientSide) host.canConnect(side) else host.sidedNode(side) != null
        }
      }

      if (tileEntity.getCapability(Capabilities.EnvironmentCapability, side).isPresent) {
        val host = tileEntity.getCapability(Capabilities.EnvironmentCapability, side)
        if (host.isPresent) return true
      }
    }

    false
  }

  private def getConnectionColor(tileEntity: TileEntity): Int = {
    if (tileEntity != null) {
      if (tileEntity.getCapability(Capabilities.ColoredCapability, null).isPresent) {
        val colored = tileEntity.getCapability(Capabilities.ColoredCapability, null).orElse(null)
        if (colored != null && colored.controlsConnectivity) return colored.getColor
      }
    }

    Color.rgbValues(DyeColor.LIGHT_GRAY)
  }

  private def canConnectBasedOnColor(te1: TileEntity, te2: TileEntity) = {
    val (c1, c2) = (getConnectionColor(te1), getConnectionColor(te2))
    c1 == c2 || c1 == Color.rgbValues(DyeColor.LIGHT_GRAY) || c2 == Color.rgbValues(DyeColor.LIGHT_GRAY)
  }

  private def canConnectFromSideIM(tileEntity: TileEntity, side: Direction) =
    tileEntity match {
      case im: tileentity.traits.ImmibisMicroblock => im.ImmibisMicroblocks_isSideOpen(side.ordinal)
      case _ => true
    }
}
