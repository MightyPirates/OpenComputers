package li.cil.oc.common.block

import li.cil.oc.Blocks
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
import net.minecraftforge.common.RotationHelper

/**
 * The base class on which all our blocks are built.
 *
 * TODO abstract away rotation (implemented in multi block) and only pass along
 * forge directions to the methods in here (which will then be "local").
 */
trait SubBlock {
  val blockId = Blocks.multi.add(this)

  def unlocalizedName: String

  // ----------------------------------------------------------------------- //
  // Block
  // ----------------------------------------------------------------------- //

  def breakBlock(world: World, x: Int, y: Int, z: Int, blockId: Int, metadata: Int) {}

  def canConnectRedstone(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) = false

  def createTileEntity(world: World, metadata: Int): TileEntity = null

  def getBlockTextureFromSide(side: Int): Icon = null

  def getCollisionBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int) =
    AxisAlignedBB.getAABBPool.getAABB(x, y, z, x + 1, y + 1, z + 1)

  def getLightOpacity(world: World, x: Int, y: Int, z: Int) = 255

  def getValidRotations(world: World, x: Int, y: Int, z: Int) =
    RotationHelper.getValidVanillaBlockRotations(Block.stone)

  def hasTileEntity(metadata: Int) = false

  def isProvidingStrongPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) = 0

  def isProvidingWeakPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) = 0

  def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = false

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

  def isBlockSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) = true

  def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
    world.isBlockOpaqueCube(x, y, z)
}