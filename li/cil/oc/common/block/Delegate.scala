package li.cil.oc.common.block

import cpw.mods.fml.relauncher.{SideOnly, Side}
import li.cil.oc.common.tileentity
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{MovingObjectPosition, Vec3, AxisAlignedBB, Icon}
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

trait Delegate {
  val unlocalizedName: String

  val showInItemList = true

  def blockId: Int

  def parent: Delegator[_]

  def setBlock(world: World, x: Int, y: Int, z: Int, flags: Int) = {
    world.setBlock(x, y, z, parent.blockID, blockId, flags)
  }

  def createItemStack(amount: Int = 1) = new ItemStack(parent, amount, itemDamage)

  // ----------------------------------------------------------------------- //

  def itemDamage = blockId

  def pick(target: MovingObjectPosition, world: World, x: Int, y: Int, z: Int): ItemStack = createItemStack()

  def drop(world: World, x: Int, y: Int, z: Int, chance: Float, fortune: Int) = false

  def isNormalCube(world: World, x: Int, y: Int, z: Int) = true

  def validRotations(world: World, x: Int, y: Int, z: Int) = validRotations_

  def canPlaceBlockOnSide(world: World, x: Int, y: Int, z: Int, side: ForgeDirection) = true

  def bounds(world: World, x: Int, y: Int, z: Int) =
    AxisAlignedBB.getAABBPool.getAABB(0, 0, 0, 1, 1, 1)

  def updateBounds(world: IBlockAccess, x: Int, y: Int, z: Int) =
    parent.setBlockBounds(0, 0, 0, 1, 1, 1)

  def intersect(world: World, x: Int, y: Int, z: Int, origin: Vec3, direction: Vec3) =
    parent.superCollisionRayTrace(world, x, y, z, origin, direction)

  // ----------------------------------------------------------------------- //

  def canConnectToRedstone(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  def isProvidingStrongPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = 0

  def isProvidingWeakPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = 0

  // ----------------------------------------------------------------------- //

  def hasTileEntity = false

  def createTileEntity(world: World): Option[TileEntity] = None

  // ----------------------------------------------------------------------- //

  def update(world: World, x: Int, y: Int, z: Int) = {}

  def addedToWorld(world: World, x: Int, y: Int, z: Int) {}

  def addedByEntity(world: World, x: Int, y: Int, z: Int, player: EntityLivingBase, stack: ItemStack) {}

  def aboutToBeRemoved(world: World, x: Int, y: Int, z: Int) =
    if (!world.isRemote) world.getBlockTileEntity(x, y, z) match {
      case inventory: tileentity.Inventory => inventory.dropAllSlots()
      case _ => // Ignore.
    }

  def removedFromWorld(world: World, x: Int, y: Int, z: Int, blockId: Int) {}

  def removedByEntity(world: World, x: Int, y: Int, z: Int, player: EntityPlayer) = true

  def neighborBlockChanged(world: World, x: Int, y: Int, z: Int, blockId: Int) {}

  def leftClick(world: World, x: Int, y: Int, z: Int, player: EntityPlayer) {}

  def rightClick(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = false

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: java.util.List[String], advanced: Boolean) {}

  def opacity(world: World, x: Int, y: Int, z: Int) = 255

  def luminance(world: IBlockAccess, x: Int, y: Int, z: Int) = 0

  @SideOnly(Side.CLIENT)
  def color = 0xFFFFFF

  @SideOnly(Side.CLIENT)
  def color(world: IBlockAccess, x: Int, y: Int, z: Int): Int = color

  @SideOnly(Side.CLIENT)
  def icon(side: ForgeDirection): Option[Icon] = None

  @SideOnly(Side.CLIENT)
  def icon(world: IBlockAccess, x: Int, y: Int, z: Int, worldSide: ForgeDirection, localSide: ForgeDirection): Option[Icon] = icon(localSide)

  def itemBounds(): Unit = parent.setBlockBoundsForItemRender()

  @SideOnly(Side.CLIENT)
  def preItemRender() {}

  @SideOnly(Side.CLIENT)
  def registerIcons(iconRegister: IconRegister) {}

  // ----------------------------------------------------------------------- //

  protected val validRotations_ = Array(
    ForgeDirection.SOUTH,
    ForgeDirection.WEST,
    ForgeDirection.NORTH,
    ForgeDirection.EAST)
}

trait SimpleDelegate extends Delegate {
  val parent: SimpleDelegator

  val blockId = parent.add(this)
}

trait SpecialDelegate extends Delegate {
  val parent: SpecialDelegator

  val blockId = parent.add(this)

  // ----------------------------------------------------------------------- //

  def isSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = true

  @SideOnly(Side.CLIENT)
  def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) =
    !world.isBlockOpaqueCube(x, y, z)
}
