package li.cil.oc.common.block

import li.cil.oc.api
import li.cil.oc.common.tileentity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection

class Keyboard(val parent: SpecialDelegator) extends SpecialDelegate {
  val unlocalizedName = "Keyboard"

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Keyboard(world.isRemote))

  override def update(world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case keyboard: tileentity.Keyboard => api.Network.joinOrCreateNetwork(keyboard)
      case _ =>
    }

  override def isBlockNormalCube(world: World, x: Int, y: Int, z: Int) = false

  override def getLightOpacity(world: World, x: Int, y: Int, z: Int) = 0

  override def setBlockBoundsBasedOnState(world: IBlockAccess, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case keyboard: tileentity.Keyboard =>
        val (forward, up) = keyboard.pitch match {
          case side@(ForgeDirection.DOWN | ForgeDirection.UP) => (side, keyboard.yaw)
          case _ => (keyboard.yaw, ForgeDirection.UP)
        }
        val side = forward.getRotation(up)
        val x0 = -forward.offsetX * 0.4f - up.offsetX * 0.2f - side.offsetX * 0.4f
        val x1 = -forward.offsetX * 0.5f + up.offsetX * 0.2f + side.offsetX * 0.4f
        val y0 = -forward.offsetY * 0.4f - up.offsetY * 0.2f - side.offsetY * 0.4f
        val y1 = -forward.offsetY * 0.5f + up.offsetY * 0.2f + side.offsetY * 0.4f
        val z0 = -forward.offsetZ * 0.4f - up.offsetZ * 0.2f - side.offsetZ * 0.4f
        val z1 = -forward.offsetZ * 0.5f + up.offsetZ * 0.2f + side.offsetZ * 0.4f
        parent.setBlockBounds(
          (x0 min x1) + 0.5f, (y0 min y1) + 0.5f, (z0 min z1) + 0.5f,
          (x0 max x1) + 0.5f, (y0 max y1) + 0.5f, (z0 max z1) + 0.5f)
      case _ => super.setBlockBoundsBasedOnState(world, x, y, z)
    }

  override def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, player: EntityLivingBase, item: ItemStack) {
    super.onBlockPlacedBy(world, x, y, z, player, item)
  }

  override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, blockId: Int) = super.onNeighborBlockChange(world, x, y, z, blockId)

  override protected val validRotations = ForgeDirection.VALID_DIRECTIONS
}