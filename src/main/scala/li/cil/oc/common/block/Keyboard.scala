package li.cil.oc.common.block

import java.util.Random

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.tileentity
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedEnumFacing._
import li.cil.oc.util.InventoryUtils
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

class Keyboard extends SimpleBlock(Material.ROCK) {
  setLightOpacity(0)

  // For Immibis Microblock support.
  val ImmibisMicroblocks_TransformableBlockMarker = null

  override def createBlockState() = new BlockStateContainer(this, PropertyRotatable.Pitch, PropertyRotatable.Yaw)

  override def getMetaFromState(state: IBlockState): Int = (state.getValue(PropertyRotatable.Pitch).ordinal() << 2) | state.getValue(PropertyRotatable.Yaw).getHorizontalIndex

  override def getStateFromMeta(meta: Int): IBlockState = getDefaultState.withProperty(PropertyRotatable.Pitch, EnumFacing.getFront(meta >> 2)).withProperty(PropertyRotatable.Yaw, EnumFacing.getHorizontal(meta & 0x3))

  // ----------------------------------------------------------------------- //

  override def isOpaqueCube(state: IBlockState): Boolean = false

  override def isFullCube(state: IBlockState): Boolean = false

  override def isSideSolid(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing) = false

  override def getBoundingBox(state: IBlockState, world: IBlockAccess, pos: BlockPos): AxisAlignedBB =
    world.getTileEntity(pos) match {
      case keyboard: tileentity.Keyboard =>
        val (pitch, yaw) = (keyboard.pitch, keyboard.yaw)
        val (forward, up) = pitch match {
          case side@(EnumFacing.DOWN | EnumFacing.UP) => (side, yaw)
          case _ => (yaw, EnumFacing.UP)
        }
        val side = forward.getRotation(up)
        val sizes = Array(7f / 16f, 4f / 16f, 7f / 16f)
        val x0 = -up.getFrontOffsetX * sizes(1) - side.getFrontOffsetX * sizes(2) - forward.getFrontOffsetX * sizes(0)
        val x1 = up.getFrontOffsetX * sizes(1) + side.getFrontOffsetX * sizes(2) - forward.getFrontOffsetX * 0.5f
        val y0 = -up.getFrontOffsetY * sizes(1) - side.getFrontOffsetY * sizes(2) - forward.getFrontOffsetY * sizes(0)
        val y1 = up.getFrontOffsetY * sizes(1) + side.getFrontOffsetY * sizes(2) - forward.getFrontOffsetY * 0.5f
        val z0 = -up.getFrontOffsetZ * sizes(1) - side.getFrontOffsetZ * sizes(2) - forward.getFrontOffsetZ * sizes(0)
        val z1 = up.getFrontOffsetZ * sizes(1) + side.getFrontOffsetZ * sizes(2) - forward.getFrontOffsetZ * 0.5f
        new AxisAlignedBB(x0, y0, z0, x1, y1, z1).offset(0.5, 0.5, 0.5)
      case _ => super.getBoundingBox(state, world, pos)
    }

  // ----------------------------------------------------------------------- //

  override def shouldSideBeRendered(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing) = true

  override def preItemRender(metadata: Int) {
    GlStateManager.translate(-0.75f, 0, 0)
    GlStateManager.scale(1.5f, 1.5f, 1.5f)
  }

  // ----------------------------------------------------------------------- //

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Keyboard()

  // ----------------------------------------------------------------------- //

  override def updateTick(world: World, pos: BlockPos, state: IBlockState, rand: Random) =
    world.getTileEntity(pos) match {
      case keyboard: tileentity.Keyboard => api.Network.joinOrCreateNetwork(keyboard)
      case _ =>
    }

  override def canPlaceBlockOnSide(world: World, pos: BlockPos, side: EnumFacing) = {
    world.isSideSolid(pos.offset(side.getOpposite), side) &&
      (world.getTileEntity(pos.offset(side.getOpposite)) match {
        case screen: tileentity.Screen => screen.facing != side
        case _ => true
      })
  }

  override def onNeighborBlockChange(world: World, pos: BlockPos, state: IBlockState, neighborBlock: Block) =
    world.getTileEntity(pos) match {
      case keyboard: tileentity.Keyboard =>
        if (!canPlaceBlockOnSide(world, pos, keyboard.facing)) {
          world.setBlockToAir(pos)
          InventoryUtils.spawnStackInWorld(BlockPosition(pos, world), api.Items.get(Constants.BlockName.Keyboard).createItemStack(1))
        }
      case _ =>
    }

  override def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, hand: EnumHand, heldItem: ItemStack, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) =
    adjacencyInfo(world, pos) match {
      case Some((keyboard, screen, blockPos, facing)) => screen.rightClick(world, blockPos, player, hand, heldItem, facing, 0, 0, 0, force = true)
      case _ => false
    }

  def adjacencyInfo(world: World, pos: BlockPos) =
    world.getTileEntity(pos) match {
      case keyboard: tileentity.Keyboard =>
        val blockPos = pos.offset(keyboard.facing.getOpposite)
        world.getBlockState(blockPos).getBlock match {
          case screen: Screen => Some((keyboard, screen, blockPos, keyboard.facing.getOpposite))
          case _ =>
            // Special case #1: check for screen in front of the keyboard.
            val forward = keyboard.facing match {
              case EnumFacing.UP | EnumFacing.DOWN => keyboard.yaw
              case _ => EnumFacing.UP
            }
            val blockPos = pos.offset(forward)
            world.getBlockState(blockPos).getBlock match {
              case screen: Screen => Some((keyboard, screen, blockPos, forward))
              case _ if keyboard.facing != EnumFacing.UP && keyboard.facing != EnumFacing.DOWN =>
                // Special case #2: check for screen below keyboards on walls.
                val blockPos = pos.offset(forward.getOpposite)
                world.getBlockState(blockPos).getBlock match {
                  case screen: Screen => Some((keyboard, screen, blockPos, forward.getOpposite))
                  case _ => None
                }
              case _ => None
            }
        }
      case _ => None
    }

  override def getValidRotations(world: World, pos: BlockPos) = null
}
