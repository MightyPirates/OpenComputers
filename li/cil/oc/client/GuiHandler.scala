package li.cil.oc.client

import li.cil.oc.common.tileentity.{Screen, Computer}
import li.cil.oc.common.{GuiHandler => CommonGuiHandler, GuiType}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World

object GuiHandler extends CommonGuiHandler {
  override def getClientGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case tileEntity: Computer if id == GuiType.Computer.id =>
        new gui.Computer(player.inventory, tileEntity)
      case tileEntity: Screen if id == GuiType.Screen.id =>
        new gui.Screen(tileEntity)
      case _ => null
    }
}
