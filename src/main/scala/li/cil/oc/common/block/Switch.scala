package li.cil.oc.common.block

import li.cil.oc.OpenComputers
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.world.World

class Switch extends SimpleBlock {
  override def hasTileEntity(state: IBlockState) = true

  override def createTileEntity(world: World, state: IBlockState) = new tileentity.Switch()

  // ----------------------------------------------------------------------- //

  override def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
    world.getTileEntity(pos) match {
      case switch: tileentity.Switch =>
        if (!player.isSneaking) {
          if (!world.isRemote) {
            player.openGui(OpenComputers, GuiType.Switch.id, world, pos.getX, pos.getY, pos.getZ)
          }
          true
        }
        else false
    }
  }
}
