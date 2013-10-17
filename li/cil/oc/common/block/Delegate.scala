package li.cil.oc.common.block

import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Icon
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

/** The base class on which all our blocks are built. */
trait Delegate {
  val unlocalizedName: String

  // ----------------------------------------------------------------------- //
  // Block
  // ----------------------------------------------------------------------- //

  def breakBlock(world: World, x: Int, y: Int, z: Int, blockId: Int, metadata: Int) {}

  def canConnectRedstone(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  def createTileEntity(world: World, metadata: Int): Option[TileEntity] = None

  def getBlockTextureFromSide(world: IBlockAccess, x: Int, y: Int, z: Int, worldSide: ForgeDirection, localSide: ForgeDirection): Option[Icon] = icon(localSide)

  def getCollisionBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int) =
    AxisAlignedBB.getAABBPool.getAABB(x, y, z, x + 1, y + 1, z + 1)

  def icon(side: ForgeDirection): Option[Icon] = None

  def getLightOpacity(world: World, x: Int, y: Int, z: Int) = 255

  def getLightValue(world: IBlockAccess, x: Int, y: Int, z: Int) = 0

  def getValidRotations(world: World, x: Int, y: Int, z: Int) = validRotations

  def hasTileEntity = false

  def isProvidingStrongPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = 0

  def isProvidingWeakPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = 0

  def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = false

  def onBlockAdded(world: World, x: Int, y: Int, z: Int) {}

  def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, player: EntityLivingBase, item: ItemStack) {}

  def onBlockPreDestroy(world: World, x: Int, y: Int, z: Int, metadata: Int) {}

  def onBlockRemovedBy(world: World, x: Int, y: Int, z: Int, player: EntityPlayer) = true

  def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, blockId: Int) {}

  def registerIcons(iconRegister: IconRegister) {}

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
}

trait SpecialDelegate extends Delegate {
  val parent: SpecialDelegator

  val blockId = parent.add(this)

  // ----------------------------------------------------------------------- //

  def isBlockNormalCube(world: World, x: Int, y: Int, z: Int) = true

  def isBlockSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = true

  def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) =
    world.isBlockOpaqueCube(x, y, z)
}
