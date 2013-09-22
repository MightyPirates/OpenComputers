package li.cil.oc.common.block

import li.cil.oc.api.INetworkNode
import net.minecraft.block.Block
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
import net.minecraftforge.common.RotationHelper

/** The base class on which all our blocks are built. */
trait SubBlock {
  def parent: BlockMulti

  def unlocalizedName: String

  val blockId = parent.add(this)

  // ----------------------------------------------------------------------- //
  // INetworkBlock
  // ----------------------------------------------------------------------- //

  def hasNode = false

  def getNode(world: IBlockAccess, x: Int, y: Int, z: Int): INetworkNode = null

  // ----------------------------------------------------------------------- //
  // Block
  // ----------------------------------------------------------------------- //

  def breakBlock(world: World, x: Int, y: Int, z: Int, blockId: Int, metadata: Int) {}

  def canConnectRedstone(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  def createTileEntity(world: World, metadata: Int): TileEntity = null

  def getBlockTextureFromSide(world: IBlockAccess, x: Int, y: Int, z: Int, worldSide: ForgeDirection, localSide: ForgeDirection): Icon = getIcon(localSide)

  def getCollisionBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int) =
    AxisAlignedBB.getAABBPool.getAABB(x, y, z, x + 1, y + 1, z + 1)

  def getIcon(side: ForgeDirection): Icon = null

  def getLightOpacity(world: World, x: Int, y: Int, z: Int) = 255

  def getValidRotations(world: World, x: Int, y: Int, z: Int) =
    RotationHelper.getValidVanillaBlockRotations(Block.stone)

  def hasTileEntity = false

  def isProvidingStrongPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = 0

  def isProvidingWeakPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = 0

  def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = false

  def onBlockAdded(world: World, x: Int, y: Int, z: Int) {}

  def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, player: EntityLivingBase, item: ItemStack) {}

  def onBlockRemovedBy(world: World, x: Int, y: Int, z: Int, player: EntityPlayer) = true

  def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, blockId: Int) {}

  def registerIcons(iconRegister: IconRegister) {}

  // ----------------------------------------------------------------------- //
  // BlockSpecialMulti
  // ----------------------------------------------------------------------- //

  // These are only called if the sub block is registered with a special multi-
  // block, meaning it supports special rendering. Normal multiblocks will
  // always be simple opaque blocks.

  def isBlockNormalCube(world: World, x: Int, y: Int, z: Int) = true

  def isBlockSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = true

  def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) =
    world.isBlockOpaqueCube(x, y, z)
}