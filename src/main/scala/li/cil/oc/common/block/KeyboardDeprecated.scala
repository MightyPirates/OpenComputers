package li.cil.oc.common.block

import java.util
import li.cil.oc.{Blocks, api, Settings}
import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import net.minecraft.block.Block
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.{MovingObjectPosition, AxisAlignedBB, IIcon}
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.util.ForgeDirection
import org.lwjgl.opengl.GL11

class KeyboardDeprecated(val parent: SpecialDelegator) extends SpecialDelegate {
  val unlocalizedName = "Keyboard"

  var icon: IIcon = null

  showInItemList = false

  // Phase over to new, proper keyboard.
  override def pick(target: MovingObjectPosition, world: World, x: Int, y: Int, z: Int) = new ItemStack(Blocks.keyboard)

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  override def icon(side: ForgeDirection) = Some(icon)

  override def registerIcons(iconRegister: IIconRegister) = {
    icon = iconRegister.registerIcon(Settings.resourceDomain + ":keyboard")
  }

  override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = true

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Keyboard(world.isRemote))

  override def update(world: World, x: Int, y: Int, z: Int) =
    world.getTileEntity(x, y, z) match {
      case keyboard: tileentity.Keyboard => api.Network.joinOrCreateNetwork(keyboard)
      case _ =>
    }

  // DEPRECATED Seems this isn't available anymore with stack info, use real
  // items in 1.7 when needed since IDs are no problem anymore.
  def canPlaceBlockOnSide(world: World, x: Int, y: Int, z: Int, side: ForgeDirection) =
    world.isSideSolid(x + side.offsetX, y + side.offsetY, z + side.offsetZ, side.getOpposite) &&
      (world.getTileEntity(x + side.offsetX, y + side.offsetY, z + side.offsetZ) match {
        case screen: tileentity.Screen => screen.facing != side.getOpposite
        case _ => true
      })

  override def isNormalCube(world: IBlockAccess, x: Int, y: Int, z: Int) = false

  override def opacity(world: IBlockAccess, x: Int, y: Int, z: Int) = 0

  override def updateBounds(world: IBlockAccess, x: Int, y: Int, z: Int) =
    world.getTileEntity(x, y, z) match {
      case keyboard: tileentity.Keyboard => parent.setBlockBounds(computeBounds(keyboard.pitch, keyboard.yaw))
      case _ => super.updateBounds(world, x, y, z)
    }

  override def itemBounds() {
    parent.setBlockBounds(computeBounds(ForgeDirection.NORTH, ForgeDirection.WEST))
  }

  override def preItemRender() {
    GL11.glTranslatef(-0.75f, 0, 0)
    GL11.glScalef(1.5f, 1.5f, 1.5f)
  }

  private def computeBounds(pitch: ForgeDirection, yaw: ForgeDirection) = {
    val (forward, up) = pitch match {
      case side@(ForgeDirection.DOWN | ForgeDirection.UP) => (side, yaw)
      case _ => (yaw, ForgeDirection.UP)
    }
    val side = forward.getRotation(up)
    val sizes = Array(7f / 16f, 4f / 16f, 7f / 16f)
    val x0 = -up.offsetX * sizes(1) - side.offsetX * sizes(2) - forward.offsetX * sizes(0)
    val x1 = up.offsetX * sizes(1) + side.offsetX * sizes(2) - forward.offsetX * 0.5f
    val y0 = -up.offsetY * sizes(1) - side.offsetY * sizes(2) - forward.offsetY * sizes(0)
    val y1 = up.offsetY * sizes(1) + side.offsetY * sizes(2) - forward.offsetY * 0.5f
    val z0 = -up.offsetZ * sizes(1) - side.offsetZ * sizes(2) - forward.offsetZ * sizes(0)
    val z1 = up.offsetZ * sizes(1) + side.offsetZ * sizes(2) - forward.offsetZ * 0.5f
    AxisAlignedBB.getBoundingBox(
      math.min(x0, x1) + 0.5f, math.min(y0, y1) + 0.5f, math.min(z0, z1) + 0.5f,
      math.max(x0, x1) + 0.5f, math.max(y0, y1) + 0.5f, math.max(z0, z1) + 0.5f)
  }

  override def neighborBlockChanged(world: World, x: Int, y: Int, z: Int, block: Block) =
    world.getTileEntity(x, y, z) match {
      case keyboard: tileentity.Keyboard if canPlaceBlockOnSide(world, x, y, z, keyboard.facing.getOpposite) => // Can stay.
      case _ =>
        parent.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0)
        world.setBlockToAir(x, y, z)
    }

  override def rightClick(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) =
    adjacencyInfo(world, x, y, z) match {
      case Some((keyboard, screen, sx, sy, sz, facing)) => screen.rightClick(world, sx, sy, sz, player, facing, 0, 0, 0)
      case _ => super.rightClick(world, x, y, z, player, side, hitX, hitY, hitZ)
    }

  def adjacentScreen(world: World, x: Int, y: Int, z: Int) =
    adjacencyInfo(world, x, y, z) match {
      case Some((_, _, sx, sy, sz, _)) => world.getTileEntity(sx, sy, sz) match {
        case screen: tileentity.Screen => Some(screen)
        case _ => None
      }
      case _ => None
    }

  def adjacencyInfo(world: World, x: Int, y: Int, z: Int) =
    world.getTileEntity(x, y, z) match {
      case keyboard: tileentity.Keyboard =>
        val (sx, sy, sz) = (
          x + keyboard.facing.getOpposite.offsetX,
          y + keyboard.facing.getOpposite.offsetY,
          z + keyboard.facing.getOpposite.offsetZ)
        Delegator.subBlock(world, sx, sy, sz) match {
          case Some(screen: Screen) => Some((keyboard, screen, sx, sy, sz, keyboard.facing.getOpposite))
          case _ =>
            // Special case #1: check for screen in front of the keyboard.
            val forward = keyboard.facing match {
              case ForgeDirection.UP | ForgeDirection.DOWN => keyboard.yaw
              case _ => ForgeDirection.UP
            }
            val (sx, sy, sz) = (
              x + forward.offsetX,
              y + forward.offsetY,
              z + forward.offsetZ)
            Delegator.subBlock(world, sx, sy, sz) match {
              case Some(screen: Screen) => Some((keyboard, screen, sx, sy, sz, forward))
              case _ if keyboard.facing != ForgeDirection.UP && keyboard.facing != ForgeDirection.DOWN =>
                // Special case #2: check for screen below keyboards on walls.
                val (sx, sy, sz) = (
                  x - forward.offsetX,
                  y - forward.offsetY,
                  z - forward.offsetZ)
                Delegator.subBlock(world, sx, sy, sz) match {
                  case Some(screen: Screen) => Some((keyboard, screen, sx, sy, sz, forward.getOpposite))
                  case _ => None
                }
              case _ => None
            }
        }
      case _ => None
    }

  override protected val validRotations_ = null
}