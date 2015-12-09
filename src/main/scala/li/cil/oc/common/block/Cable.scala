package li.cil.oc.common.block

/* TODO FMP
import codechicken.lib.vec.Cuboid6
import codechicken.multipart.JNormalOcclusion
import codechicken.multipart.NormalOcclusionTest
import codechicken.multipart.TFacePart
import codechicken.multipart.TileMultipart
import li.cil.oc.integration.fmp.CablePart
*/

import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.SidedComponent
import li.cil.oc.api.network.SidedEnvironment
import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import net.minecraft.block.Block
import net.minecraft.block.properties.IProperty
import net.minecraft.block.state.IBlockState
import net.minecraft.item.EnumDyeColor
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.property.IExtendedBlockState
import net.minecraftforge.common.property.IUnlistedProperty
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

import scala.collection.mutable.ArrayBuffer

class Cable extends SimpleBlock with traits.Extended {
  // For Immibis Microblock support.
  val ImmibisMicroblocks_TransformableBlockMarker = null

  // For FMP part coloring.
  var colorMultiplierOverride: Option[Int] = None

  // ----------------------------------------------------------------------- //

  override protected def setDefaultExtendedState(state: IBlockState) = setDefaultState(state)

  override protected def addExtendedState(state: IBlockState, world: IBlockAccess, pos: BlockPos) =
    (state, world.getTileEntity(pos)) match {
      case (extendedState: IExtendedBlockState, cable: tileentity.Cable) =>
        super.addExtendedState(extendedState.withProperty(property.PropertyTile.Tile, cable), world, pos)
      case _ => None
    }

  override protected def createProperties(listed: ArrayBuffer[IProperty[_ <: Comparable[AnyRef]]], unlisted: ArrayBuffer[IUnlistedProperty[_ <: Comparable[AnyRef]]]) {
    super.createProperties(listed, unlisted)
    unlisted += property.PropertyTile.Tile.asInstanceOf[IUnlistedProperty[_ <: Comparable[AnyRef]]]
  }

  // ----------------------------------------------------------------------- //

  override def isOpaqueCube = false

  override def isFullCube = false

  @SideOnly(Side.CLIENT) override
  def colorMultiplier(world: IBlockAccess, pos: BlockPos, renderPass: Int) = colorMultiplierOverride.getOrElse(super.colorMultiplier(world, pos, renderPass))

  override def shouldSideBeRendered(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = true

  override def isSideSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = false

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(state: IBlockState) = true

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Cable()

  // ----------------------------------------------------------------------- //

  override def onNeighborBlockChange(world: World, pos: BlockPos, state: IBlockState, neighborBlock: Block) {
    world.markBlockForUpdate(pos)
    super.onNeighborBlockChange(world, pos, state, neighborBlock)
  }

  override def setBlockBoundsBasedOnState(world: IBlockAccess, pos: BlockPos): Unit = {
    setBlockBounds(Cable.bounds(world, pos))
  }
}

object Cable {
  val cachedBounds = {
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

  def neighbors(world: IBlockAccess, pos: BlockPos) = {
    var result = 0
    val tileEntity = world.getTileEntity(pos)
    for (side <- EnumFacing.values) {
      val tpos = pos.offset(side)
      if (world match {
        case world: World => world.isBlockLoaded(tpos)
        case _ => !world.isAirBlock(tpos)
      }) {
        val neighborTileEntity = world.getTileEntity(tpos)
        if (neighborTileEntity != null && neighborTileEntity.getWorld != null) {
          val neighborHasNode = hasNetworkNode(neighborTileEntity, side.getOpposite)
          val canConnectColor = canConnectBasedOnColor(tileEntity, neighborTileEntity)
          val canConnectFMP = !Mods.ForgeMultipart.isAvailable ||
            (canConnectFromSideFMP(tileEntity, side) && canConnectFromSideFMP(neighborTileEntity, side.getOpposite))
          val canConnectIM = canConnectFromSideIM(tileEntity, side) && canConnectFromSideIM(neighborTileEntity, side.getOpposite)
          if (neighborHasNode && canConnectColor && canConnectFMP && canConnectIM) {
            result |= (1 << side.getIndex)
          }
        }
      }
    }
    result
  }

  def bounds(world: IBlockAccess, pos: BlockPos) = Cable.cachedBounds(Cable.neighbors(world, pos))

  private def hasNetworkNode(tileEntity: TileEntity, side: EnumFacing) =
    tileEntity match {
      case robot: tileentity.RobotProxy => false
      case host: SidedEnvironment =>
        if (host.getWorld.isRemote) host.canConnect(side)
        else host.sidedNode(side) != null
      case host: Environment with SidedComponent =>
        host.canConnectNode(side)
      case host: Environment => true
      case host if Mods.ForgeMultipart.isAvailable => hasMultiPartNode(tileEntity)
      case _ => false
    }

  private def hasMultiPartNode(tileEntity: TileEntity) = false

  /* TODO FMP
    tileEntity match {
      case host: TileMultipart => host.partList.exists(_.isInstanceOf[CablePart])
      case _ => false
    }
  */

  private def cableColor(tileEntity: TileEntity) =
    tileEntity match {
      case cable: tileentity.Cable => cable.color
      case _ =>
        if (Mods.ForgeMultipart.isAvailable) cableColorFMP(tileEntity)
        else EnumDyeColor.SILVER
    }

  private def cableColorFMP(tileEntity: TileEntity) = EnumDyeColor.SILVER

  /* TODO FMP
    tileEntity match {
      case host: TileMultipart => (host.partList collect {
        case cable: CablePart => cable.color
      }).headOption.getOrElse(Color.LightGray)
      case _ => Color.LightGray
    }
  */

  private def canConnectBasedOnColor(te1: TileEntity, te2: TileEntity) = {
    val (c1, c2) = (cableColor(te1), cableColor(te2))
    c1 == c2 || c1 == EnumDyeColor.SILVER || c2 == EnumDyeColor.SILVER
  }

  private def canConnectFromSideFMP(tileEntity: TileEntity, side: EnumFacing) = true

  /* TODO FMP
    tileEntity match {
      case host: TileMultipart =>
        host.partList.forall {
          case part: JNormalOcclusion if !part.isInstanceOf[CablePart] =>
            import scala.collection.convert.WrapAsScala._
            val ownBounds = Iterable(new Cuboid6(cachedBounds(side.flag)))
            val otherBounds = part.getOcclusionBoxes
            NormalOcclusionTest(ownBounds, otherBounds)
          case part: TFacePart => !part.solid(side.ordinal) || (part.getSlotMask & codechicken.multipart.PartMap.face(side.ordinal).mask) == 0
          case _ => true
        }
      case _ => true
    }
  */

  private def canConnectFromSideIM(tileEntity: TileEntity, side: EnumFacing) =
    tileEntity match {
      case im: tileentity.traits.ImmibisMicroblock => im.ImmibisMicroblocks_isSideOpen(side.ordinal)
      case _ => true
    }
}