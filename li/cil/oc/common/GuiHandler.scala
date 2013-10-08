package li.cil.oc.common

import cpw.mods.fml.common.network.IGuiHandler
import li.cil.oc.common.tileentity.Computer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World

abstract class GuiHandler extends IGuiHandler {
  override def getServerGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case tileEntity: Computer =>
        new container.Computer(player.inventory, tileEntity)
      case _ => null
    }
}