package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.api.component.RackMountable
import li.cil.oc.common.GuiType
import li.cil.oc.common.block.property.PropertyRotatable
import li.cil.oc.common.tileentity
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumFacing.Axis
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.property.ExtendedBlockState
import net.minecraftforge.common.property.IExtendedBlockState

class Rack extends RedstoneAware with traits.PowerAcceptor with traits.StateAware with traits.GUI {
  override def createBlockState() = new ExtendedBlockState(this, Array(PropertyRotatable.Facing), Array(property.PropertyTile.Tile))

  override def getStateFromMeta(meta: Int): IBlockState = getDefaultState.withProperty(PropertyRotatable.Facing, EnumFacing.getHorizontal(meta))

  override def getMetaFromState(state: IBlockState): Int = state.getValue(PropertyRotatable.Facing).getHorizontalIndex

  override def getExtendedState(state: IBlockState, world: IBlockAccess, pos: BlockPos): IBlockState = {
    ((state, world.getTileEntity(pos)) match {
      case (extendedState: IExtendedBlockState, tile: tileentity.traits.TileEntity) =>
        extendedState.withProperty(property.PropertyTile.Tile, tile)
      case _ => state
    }).withProperty(PropertyRotatable.Facing, getFacing(world, pos))
  }

//  @SideOnly(Side.CLIENT)
//  override def getMixedBrightnessForBlock(world: IBlockAccess, pos: BlockPos) = {
//    if (pos.getY >= 0 && pos.getY < 256) world.getTileEntity(pos) match {
//      case rack: tileentity.Rack =>
//        def brightness(pos: BlockPos) = world.getCombinedLight(pos, world.getBlockState(pos).getBlock.getLightValue(world, pos))
//        val value = brightness(pos.offset(rack.facing))
//        val skyBrightness = (value >> 20) & 15
//        val blockBrightness = (value >> 4) & 15
//        ((skyBrightness * 3 / 4) << 20) | ((blockBrightness * 3 / 4) << 4)
//      case _ => super.getMixedBrightnessForBlock(world, pos)
//    }
//    else super.getMixedBrightnessForBlock(world, pos)
//  }

  override def isOpaqueCube(state: IBlockState): Boolean = false

  override def isFullCube(state: IBlockState): Boolean = false

  override def isBlockSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = side == EnumFacing.SOUTH

  override def isSideSolid(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing) = toLocal(world, pos, side) != EnumFacing.SOUTH

  // ----------------------------------------------------------------------- //

  override def energyThroughput = Settings.get.serverRackRate

  override def guiType = GuiType.Rack

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Rack()

  // ----------------------------------------------------------------------- //

  override def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, hand: EnumHand, heldItem: ItemStack, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    world.getTileEntity(pos) match {
      case rack: tileentity.Rack => rack.slotAt(side, hitX, hitY, hitZ) match {
        case Some(slot) =>
          val hitVec = new Vec3d(hitX, hitY, hitZ)
          val rotation = side match {
            case EnumFacing.WEST => Math.toRadians(90).toFloat
            case EnumFacing.NORTH => Math.toRadians(180).toFloat
            case EnumFacing.EAST => Math.toRadians(270).toFloat
            case _ => 0
          }
          val rotatedHitVec = rotate(hitVec.addVector(-0.5, -0.5, -0.5), rotation).addVector(0.5, 0.5, 0.5)
          val x = ((if (side.getAxis != Axis.Z) 1 - rotatedHitVec.xCoord else rotatedHitVec.xCoord) * 16 - 1) / 14f
          val y = ((1 - rotatedHitVec.yCoord) * 16 - 2 - 3 * slot) / 3f
          rack.getMountable(slot) match {
            case mountable: RackMountable if mountable.onActivate(player, x.toFloat, y.toFloat) => return true // Activation handled by mountable.
            case _ =>
          }
        case _ =>
      }
      case _ =>
    }
    super.localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ)
  }

  def rotate(v: Vec3d, t: Float): Vec3d = {
    val cos = Math.cos(t)
    val sin = Math.sin(t)
    new Vec3d(v.xCoord * cos - v.zCoord * sin, v.yCoord, v.xCoord * sin + v.zCoord * cos)
  }
}
