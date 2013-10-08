package li.cil.oc.server

import li.cil.oc.common.{GuiHandler => CommonGuiHandler}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World

object GuiHandler extends CommonGuiHandler {
  override def getClientGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int) = null
}
