package li.cil.oc.common.block

import codechicken.lib.vec.Cuboid6
import codechicken.multipart.JNormalOcclusion
import codechicken.multipart.NormalOcclusionTest
import codechicken.multipart.TFacePart
import codechicken.multipart.TileMultipart
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.SidedComponent
import li.cil.oc.api.network.SidedEnvironment
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import li.cil.oc.integration.fmp.CablePart
import li.cil.oc.util.Color
import net.minecraft.block.Block
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.MovingObjectPosition
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

import scala.reflect.ClassTag

class Cable(protected implicit val tileTag: ClassTag[tileentity.Cable]) extends SimpleBlock with traits.SpecialBlock with traits.CustomDrops[tileentity.Cable] {
  setLightOpacity(0)

  // For Immibis Microblock support.
  val ImmibisMicroblocks_TransformableBlockMarker = null

  // For FMP part coloring.
  var colorMultiplierOverride: Option[Int] = None

  override protected def customTextures = Array(
    Some("CablePart"),
    Some("CablePart"),
    Some("CablePart"),
    Some("CablePart"),
    Some("CablePart"),
    Some("CablePart")
  )

  @SideOnly(Side.CLIENT)
  override def registerBlockIcons(iconRegister: IIconRegister) {
    super.registerBlockIcons(iconRegister)
    Textures.Cable.iconCap = iconRegister.registerIcon(Settings.resourceDomain + ":CableCap")
  }

  override def colorMultiplier(world: IBlockAccess, x: Int, y: Int, z: Int) =
    colorMultiplierOverride.getOrElse(super.colorMultiplier(world, x, y, z))

  override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = true

  override def isSideSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  @SideOnly(Side.CLIENT)
  override def getRenderColor(metadata: Int) = metadata

  // ----------------------------------------------------------------------- //

  override def getPickBlock(target: MovingObjectPosition, world: World, x: Int, y: Int, z: Int) =
    world.getTileEntity(x, y, z) match {
      case t: tileentity.Cable => t.createItemStack()
      case _ => null
    }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Cable()

  // ----------------------------------------------------------------------- //

  override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, block: Block) {
    world.markBlockForUpdate(x, y, z)
    super.onNeighborBlockChange(world, x, y, z, block)
  }

  override protected def doSetBlockBoundsBasedOnState(world: IBlockAccess, x: Int, y: Int, z: Int): Unit = {
    setBlockBounds(Cable.bounds(world, x, y, z))
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
      dropBlockAsItem(tileEntity.world, tileEntity.x, tileEntity.y, tileEntity.z, tileEntity.createItemStack())
    }
  }
}

object Cable {
  val cachedBounds = {
    // 6 directions = 6 bits = 11111111b >> 2 = 0xFF >> 2
    (0 to 0xFF >> 2).map(mask => {
      val bounds = AxisAlignedBB.getBoundingBox(-0.125, -0.125, -0.125, 0.125, 0.125, 0.125)
      for (side <- ForgeDirection.VALID_DIRECTIONS) {
        if ((side.flag & mask) != 0) {
          if (side.offsetX < 0) bounds.minX += side.offsetX * 0.375
          else bounds.maxX += side.offsetX * 0.375
          if (side.offsetY < 0) bounds.minY += side.offsetY * 0.375
          else bounds.maxY += side.offsetY * 0.375
          if (side.offsetZ < 0) bounds.minZ += side.offsetZ * 0.375
          else bounds.maxZ += side.offsetZ * 0.375
        }
      }
      bounds.setBounds(
        bounds.minX + 0.5, bounds.minY + 0.5, bounds.minZ + 0.5,
        bounds.maxX + 0.5, bounds.maxY + 0.5, bounds.maxZ + 0.5)
    }).toArray
  }

  def neighbors(world: IBlockAccess, x: Int, y: Int, z: Int) = {
    var result = 0
    val tileEntity = world.getTileEntity(x, y, z)
    for (side <- ForgeDirection.VALID_DIRECTIONS) {
      val (tx, ty, tz) = (x + side.offsetX, y + side.offsetY, z + side.offsetZ)
      if (world match {
        case world: World => world.blockExists(tx, ty, tz)
        case _ => !world.isAirBlock(tx, ty, tz)
      }) {
        val neighborTileEntity = world.getTileEntity(tx, ty, tz)
        val neighborHasNode = hasNetworkNode(neighborTileEntity, side.getOpposite)
        val canConnectColor = canConnectBasedOnColor(tileEntity, neighborTileEntity)
        val canConnectFMP = !Mods.ForgeMultipart.isAvailable ||
          (canConnectFromSideFMP(tileEntity, side) && canConnectFromSideFMP(neighborTileEntity, side.getOpposite))
        val canConnectIM = canConnectFromSideIM(tileEntity, side) && canConnectFromSideIM(neighborTileEntity, side.getOpposite)
        if (neighborHasNode && canConnectColor && canConnectFMP && canConnectIM) {
          result |= side.flag
        }
      }
    }
    result
  }

  def bounds(world: IBlockAccess, x: Int, y: Int, z: Int) = Cable.cachedBounds(Cable.neighbors(world, x, y, z)).copy()

  private def hasNetworkNode(tileEntity: TileEntity, side: ForgeDirection) =
    tileEntity match {
      case robot: tileentity.RobotProxy => false
      case host: SidedEnvironment =>
        if (host.getWorldObj.isRemote) host.canConnect(side)
        else host.sidedNode(side) != null
      case host: Environment with SidedComponent =>
        host.canConnectNode(side)
      case host: Environment => true
      case host if Mods.ForgeMultipart.isAvailable => hasMultiPartNode(tileEntity)
      case _ => false
    }

  private def hasMultiPartNode(tileEntity: TileEntity) =
    tileEntity match {
      case host: TileMultipart => host.partList.exists(_.isInstanceOf[CablePart])
      case _ => false
    }

  private def cableColor(tileEntity: TileEntity) =
    tileEntity match {
      case cable: tileentity.Cable => cable.color
      case _ =>
        if (Mods.ForgeMultipart.isAvailable) cableColorFMP(tileEntity)
        else Color.LightGray
    }

  private def cableColorFMP(tileEntity: TileEntity) =
    tileEntity match {
      case host: TileMultipart => (host.partList collect {
        case cable: CablePart => cable.color
      }).headOption.getOrElse(Color.LightGray)
      case _ => Color.LightGray
    }

  private def canConnectBasedOnColor(te1: TileEntity, te2: TileEntity) = {
    val (c1, c2) = (cableColor(te1), cableColor(te2))
    c1 == c2 || c1 == Color.LightGray || c2 == Color.LightGray
  }

  private def canConnectFromSideFMP(tileEntity: TileEntity, side: ForgeDirection) =
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

  private def canConnectFromSideIM(tileEntity: TileEntity, side: ForgeDirection) =
    tileEntity match {
      case im: tileentity.traits.ImmibisMicroblock => im.ImmibisMicroblocks_isSideOpen(side.ordinal)
      case _ => true
    }
}