package li.cil.oc.common.block

import java.util
import li.cil.oc.Config
import li.cil.oc.api
import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.{AxisAlignedBB, Icon}
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection
import org.lwjgl.opengl.GL11

class Keyboard(val parent: SpecialDelegator) extends SpecialDelegate {
  val unlocalizedName = "Keyboard"

  var icon: Icon = null

  override def addInformation(player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(unlocalizedName))
  }

  override def icon(side: ForgeDirection) = Some(icon)

  override def registerIcons(iconRegister: IconRegister) = {
    icon = iconRegister.registerIcon(Config.resourceDomain + ":keyboard")
  }

  override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = true

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Keyboard(world.isRemote))

  override def update(world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case keyboard: tileentity.Keyboard => api.Network.joinOrCreateNetwork(keyboard)
      case _ =>
    }

  override def canPlaceBlockOnSide(world: World, x: Int, y: Int, z: Int, side: ForgeDirection) =
    world.getBlockTileEntity(x + side.offsetX, y + side.offsetY, z + side.offsetZ) match {
      case screen: tileentity.Screen => screen.facing != side.getOpposite
      case _ => false
    }

  override def isBlockNormalCube(world: World, x: Int, y: Int, z: Int) = false

  override def getLightOpacity(world: World, x: Int, y: Int, z: Int) = 0

  override def setBlockBoundsBasedOnState(world: IBlockAccess, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case keyboard: tileentity.Keyboard => parent.setBlockBounds(computeBounds(keyboard.pitch, keyboard.yaw))
      case _ => super.setBlockBoundsBasedOnState(world, x, y, z)
    }

  override def setBlockBoundsForItemRender() {
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
      (x0 min x1) + 0.5f, (y0 min y1) + 0.5f, (z0 min z1) + 0.5f,
      (x0 max x1) + 0.5f, (y0 max y1) + 0.5f, (z0 max z1) + 0.5f)
  }

  override def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, player: EntityLivingBase, item: ItemStack) {
    super.onBlockPlacedBy(world, x, y, z, player, item)
  }

  override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, blockId: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case keyboard: tileentity.Keyboard if canPlaceBlockOnSide(world, x, y, z, keyboard.facing.getOpposite) => // Can stay.
      case _ =>
        parent.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0)
        world.setBlockToAir(x, y, z)
    }

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) =
    world.getBlockTileEntity(x, y, z) match {
      case keyboard: tileentity.Keyboard =>
        val (sx, sy, sz) = (
          x + keyboard.facing.getOpposite.offsetX,
          y + keyboard.facing.getOpposite.offsetY,
          z + keyboard.facing.getOpposite.offsetZ)
        Delegator.subBlock(world, sx, sy, sz) match {
          case Some(screen: Screen) => screen.onBlockActivated(world, sx, sy, sz, player, keyboard.facing.getOpposite, 0, 0, 0)
          case _ => super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ)
        }
      case _ => super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ)
    }

  override protected val validRotations = ForgeDirection.VALID_DIRECTIONS
}