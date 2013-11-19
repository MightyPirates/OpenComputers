package li.cil.oc.common.block

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

/** The base class on which all our blocks are built. */
trait Delegate {
  val unlocalizedName: String

  // ----------------------------------------------------------------------- //
  // Block
  // ----------------------------------------------------------------------- //

  def breakBlock(world: World, x: Int, y: Int, z: Int, blockId: Int) {}

  def canConnectRedstone(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  def colorMultiplier(world: IBlockAccess, x: Int, y: Int, z: Int) = getRenderColor

  def collisionRayTrace(world: World, x: Int, y: Int, z: Int, origin: Vec3, direction: Vec3): MovingObjectPosition

  def createTileEntity(world: World): Option[TileEntity] = None

  def getBlockTextureFromSide(world: IBlockAccess, x: Int, y: Int, z: Int,
                              worldSide: ForgeDirection, localSide: ForgeDirection): Option[Icon] = icon(localSide)

  def getCollisionBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int) =
    AxisAlignedBB.getAABBPool.getAABB(0, 0, 0, 1, 1, 1)

  def getRenderColor = 0xFFFFFF

  def getLightOpacity(world: World, x: Int, y: Int, z: Int) = 255

  def getLightValue(world: IBlockAccess, x: Int, y: Int, z: Int) = 0

  def getValidRotations(world: World, x: Int, y: Int, z: Int) = validRotations

  def hasTileEntity = false

  def icon(side: ForgeDirection): Option[Icon] = None

  def isBlockNormalCube(world: World, x: Int, y: Int, z: Int) = true

  def isProvidingStrongPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = 0

  def isProvidingWeakPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = 0

  def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = false

  def onBlockAdded(world: World, x: Int, y: Int, z: Int) {}

  def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, player: EntityLivingBase, item: ItemStack) {}

  def onBlockPreDestroy(world: World, x: Int, y: Int, z: Int) =
    if (!world.isRemote) world.getBlockTileEntity(x, y, z) match {
      case inventory: tileentity.Inventory => inventory.dropAllSlots()
      case _ => // Ignore.
    }

  def onBlockRemovedBy(world: World, x: Int, y: Int, z: Int, player: EntityPlayer) = true

  def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, blockId: Int) {}

  def registerIcons(iconRegister: IconRegister) {}

  def setBlockBoundsBasedOnState(world: IBlockAccess, x: Int, y: Int, z: Int)

  // ----------------------------------------------------------------------- //

  private val validRotations = Array(
    ForgeDirection.SOUTH,
    ForgeDirection.WEST,
    ForgeDirection.NORTH,
    ForgeDirection.EAST)
}

trait SimpleDelegate extends Delegate {
  val parent: SimpleDelegator

  val blockId = parent.add(this)

  def collisionRayTrace(world: World, x: Int, y: Int, z: Int, origin: Vec3, direction: Vec3) =
    parent.superCollisionRayTrace(world, x, y, z, origin, direction)

  def setBlockBoundsBasedOnState(world: IBlockAccess, x: Int, y: Int, z: Int) =
    parent.setBlockBounds(0, 0, 0, 1, 1, 1)
}

trait SpecialDelegate extends Delegate {
  val parent: SpecialDelegator

  val blockId = parent.add(this)

  // ----------------------------------------------------------------------- //

  def collisionRayTrace(world: World, x: Int, y: Int, z: Int, origin: Vec3, direction: Vec3) =
    parent.superCollisionRayTrace(world, x, y, z, origin, direction)

  def isBlockSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = true

  def setBlockBoundsBasedOnState(world: IBlockAccess, x: Int, y: Int, z: Int) =
    parent.setBlockBounds(0, 0, 0, 1, 1, 1)

  def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) =
    !world.isBlockOpaqueCube(x, y, z)
}
