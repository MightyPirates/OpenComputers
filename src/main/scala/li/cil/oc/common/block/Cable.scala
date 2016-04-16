package li.cil.oc.common.block

import li.cil.oc.common.capabilities.Capabilities
import li.cil.oc.common.tileentity
import li.cil.oc.util.Color
import net.minecraft.block.Block
import net.minecraft.block.state.BlockState
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovingObjectPosition
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.property.ExtendedBlockState
import net.minecraftforge.common.property.IExtendedBlockState
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

import scala.reflect.ClassTag

class Cable(protected implicit val tileTag: ClassTag[tileentity.Cable]) extends SimpleBlock with traits.CustomDrops[tileentity.Cable] {
  // For Immibis Microblock support.
  val ImmibisMicroblocks_TransformableBlockMarker = null

  // For FMP part coloring.
  var colorMultiplierOverride: Option[Int] = None

  // ----------------------------------------------------------------------- //

  override def createBlockState(): BlockState = new ExtendedBlockState(this, Array.empty, Array(property.PropertyTile.Tile))

  override def getExtendedState(state: IBlockState, world: IBlockAccess, pos: BlockPos): IBlockState =
    (state, world.getTileEntity(pos)) match {
      case (extendedState: IExtendedBlockState, cable: tileentity.Cable) =>
        extendedState.withProperty(property.PropertyTile.Tile, cable)
      case _ => state
    }

  // ----------------------------------------------------------------------- //

  override def isOpaqueCube = false

  override def isFullCube = false

  @SideOnly(Side.CLIENT) override
  def colorMultiplier(world: IBlockAccess, pos: BlockPos, renderPass: Int) = colorMultiplierOverride.getOrElse(super.colorMultiplier(world, pos, renderPass))

  override def shouldSideBeRendered(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = true

  override def isSideSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = false

  // ----------------------------------------------------------------------- //

  override def getPickBlock(target: MovingObjectPosition, world: World, pos: BlockPos) =
    world.getTileEntity(pos) match {
      case t: tileentity.Cable => t.createItemStack()
      case _ => null
    }

  // ----------------------------------------------------------------------- //

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Cable()

  // ----------------------------------------------------------------------- //

  override def onNeighborBlockChange(world: World, pos: BlockPos, state: IBlockState, neighborBlock: Block) {
    world.markBlockForUpdate(pos)
    super.onNeighborBlockChange(world, pos, state, neighborBlock)
  }

  override def setBlockBoundsBasedOnState(world: IBlockAccess, pos: BlockPos): Unit = {
    setBlockBounds(Cable.bounds(world, pos))
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
  final val CachedBounds = {
    // 6 directions = 6 bits = 11111111b >> 2 = 0xFF >> 2
    (0 to 0xFF >> 2).map(mask => {
      var minX = -0.125
      var minY = -0.125
      var minZ = -0.125
      var maxX = 0.125
      var maxY = 0.125
      var maxZ = 0.125
      for (side <- EnumFacing.values) {
        if (((1 << side.getIndex) & mask) != 0) {
          if (side.getFrontOffsetX < 0) minX += side.getFrontOffsetX * 0.375
          else maxX += side.getFrontOffsetX * 0.375
          if (side.getFrontOffsetY < 0) minY += side.getFrontOffsetY * 0.375
          else maxY += side.getFrontOffsetY * 0.375
          if (side.getFrontOffsetZ < 0) minZ += side.getFrontOffsetZ * 0.375
          else maxZ += side.getFrontOffsetZ * 0.375
        }
      }
      AxisAlignedBB.fromBounds(
        minX + 0.5, minY + 0.5, minZ + 0.5,
        maxX + 0.5, maxY + 0.5, maxZ + 0.5)
    }).toArray
  }
  final val DefaultBounds = CachedBounds(0)

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