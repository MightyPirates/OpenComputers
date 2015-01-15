package li.cil.oc.common.block.traits

import li.cil.oc.OpenComputers
import li.cil.oc.common.GuiType
import li.cil.oc.common.block.SimpleBlock
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

trait GUI extends SimpleBlock {
  def guiType: GuiType.EnumVal

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking) {
      if (!world.isRemote) {
        player.openGui(OpenComputers, guiType.id, world, x, y, z)
      }
      true
    }
    else super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ)
  }
}
