package li.cil.oc.common.block.traits

import li.cil.oc.OpenComputers
import li.cil.oc.common.GuiType
import li.cil.oc.common.block.SimpleBlock
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.world.World

trait GUI extends SimpleBlock {
  def guiType: GuiType.EnumVal

  override def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    if (!player.isSneaking) {
      if (!world.isRemote) {
        player.openGui(OpenComputers, guiType.id, world, pos.getX, pos.getY, pos.getZ)
      }
      true
    }
    else super.localOnBlockActivated(world, pos, player, side, hitX, hitY, hitZ)
  }
}
