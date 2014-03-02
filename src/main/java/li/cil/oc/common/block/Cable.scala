package li.cil.oc.common.block

import cpw.mods.fml.relauncher.{SideOnly, Side}
import java.util
import li.cil.oc.api.network.{SidedEnvironment, Environment}
import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import li.cil.oc.{Settings, api}
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.{Icon, AxisAlignedBB}
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection
import codechicken.multipart.{TileMultipart, TMultiPart}
import li.cil.oc.common.multipart.CablePart

class Cable(val parent: SpecialDelegator) extends SpecialDelegate {
  val unlocalizedName = "Cable"

  private var icon: Icon = _

  // ----------------------------------------------------------------------- //

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  @SideOnly(Side.CLIENT)
  override def icon(side: ForgeDirection) = Some(icon)

  @SideOnly(Side.CLIENT)
  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)
    icon = iconRegister.registerIcon(Settings.resourceDomain + ":generic_top")
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Cable)

  // ----------------------------------------------------------------------- //

  override def isNormalCube(world: World, x: Int, y: Int, z: Int) = false

  override def isSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  override def opacity(world: World, x: Int, y: Int, z: Int) = 0

  override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  // ----------------------------------------------------------------------- //

  override def neighborBlockChanged(world: World, x: Int, y: Int, z: Int, blockId: Int) {
    world.markBlockForRenderUpdate(x, y, z)
    super.neighborBlockChanged(world, x, y, z, blockId)
  }

  override def updateBounds(world: IBlockAccess, x: Int, y: Int, z: Int) {
    parent.setBlockBounds(Cable.bounds(world, x, y, z))
  }
}

object Cable {
  private val cachedBounds = {
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
    val tileEntity = world.getBlockTileEntity(x, y, z) match {
      case multi: TileMultipart => Some(multi)
      case _ => None
    }
    for (side <- ForgeDirection.VALID_DIRECTIONS) {
      if (tileEntity match {
        case Some(multi) => !multi.isSolid(side.ordinal)
        case _ => true
      }) {
        val (tx, ty, tz) = (x + side.offsetX, y + side.offsetY, z + side.offsetZ)
        if (!world.isAirBlock(tx, ty, tz)) world.getBlockTileEntity(tx, ty, tz) match {
          case robot: tileentity.RobotProxy =>
          case host: SidedEnvironment =>
            val connects = if (host.getWorldObj.isRemote) host.canConnect(side.getOpposite) else host.sidedNode(side.getOpposite) != null
            if (connects) {
              result |= side.flag
            }
          case host: Environment => result |= side.flag
          case multi: TileMultipart =>
            if (multi.partList.exists(_.isInstanceOf[CablePart]) &&
              !multi.isSolid(side.getOpposite.ordinal))
              result |= side.flag
          case _ =>
        }
      }
    }
    result
  }

  def bounds(world: IBlockAccess, x: Int, y: Int, z: Int) = Cable.cachedBounds(Cable.neighbors(world, x, y, z)).copy()
}