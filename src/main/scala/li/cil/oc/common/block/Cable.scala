package li.cil.oc.common.block

import cpw.mods.fml.common.Loader
import cpw.mods.fml.relauncher.{SideOnly, Side}
import java.util
import li.cil.oc.api.network.{SidedEnvironment, Environment}
import li.cil.oc.common.tileentity
import li.cil.oc.Settings
import li.cil.oc.util.Tooltip
import net.minecraft.block.Block
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.IIcon
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.util.ForgeDirection

class Cable(val parent: SpecialDelegator) extends SpecialDelegate {
  val unlocalizedName = "Cable"

  private var icon: IIcon = _

  // ----------------------------------------------------------------------- //

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  @SideOnly(Side.CLIENT)
  override def icon(side: ForgeDirection) = Some(icon)

  @SideOnly(Side.CLIENT)
  override def registerIcons(iconRegister: IIconRegister) {
    super.registerIcons(iconRegister)
    icon = iconRegister.registerIcon(Settings.resourceDomain + ":generic_top")
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Cable)

  // ----------------------------------------------------------------------- //

  override def isNormalCube(world: IBlockAccess, x: Int, y: Int, z: Int) = false

  override def isSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  override def opacity(world: IBlockAccess, x: Int, y: Int, z: Int) = 0

  override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  // ----------------------------------------------------------------------- //

  override def neighborBlockChanged(world: World, x: Int, y: Int, z: Int, block: Block) {
    world.markBlockForUpdate(x, y, z)
    super.neighborBlockChanged(world, x, y, z, block)
  }

  override def updateBounds(world: IBlockAccess, x: Int, y: Int, z: Int) {
    parent.setBlockBounds(Cable.bounds(world, x, y, z))
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
      if (!world.isAirBlock(tx, ty, tz)) {
        val neighborTileEntity = world.getTileEntity(tx, ty, tz)
        val neighborHasNode = hasNetworkNode(neighborTileEntity, side.getOpposite)
        val canConnect = !Loader.isModLoaded("ForgeMultipart") ||
          (canConnectFromSide(tileEntity, side) && canConnectFromSide(neighborTileEntity, side.getOpposite))
        if (neighborHasNode && canConnect) {
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
      case host: Environment => true
      case host if Loader.isModLoaded("ForgeMultipart") => hasMultiPartNode(tileEntity)
      case _ => false
    }

  private def hasMultiPartNode(tileEntity: TileEntity) =
    tileEntity match {
      /* TODO FMP
      case host: TileMultipart => host.partList.exists(_.isInstanceOf[CablePart])
      */
      case _ => false
    }

  private def canConnectFromSide(tileEntity: TileEntity, side: ForgeDirection) =
    tileEntity match {
      /* TODO FMP
      case host: TileMultipart =>
        !host.partList.exists {
          case part: JNormalOcclusion if !part.isInstanceOf[CablePart] =>
            import scala.collection.convert.WrapAsScala._
            val ownBounds = Iterable(new Cuboid6(cachedBounds(side.flag)))
            val otherBounds = part.getOcclusionBoxes
            !NormalOcclusionTest(ownBounds, otherBounds)
          case _ => false
        }
      */
      case _ => true
    }
}