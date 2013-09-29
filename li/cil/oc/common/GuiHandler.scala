package li.cil.oc.common

import cpw.mods.fml.common.network.IGuiHandler
import li.cil.oc.client.gui
import li.cil.oc.common.tileentity.Computer
import li.cil.oc.common.tileentity.Screen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World

object GuiType extends Enumeration {
  val Computer = Value("Computer")
  val Screen = Value("Screen")
}

object GuiHandler extends IGuiHandler {
  override def getServerGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case tileEntity: Computer =>
        new container.Computer(player.inventory, tileEntity)
      case _ => null
    }

  override def getClientGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case tileEntity: Computer if id == GuiType.Computer.id =>
        new gui.Computer(player.inventory, tileEntity)
      case tileEntity: Screen if id == GuiType.Screen.id =>
        new gui.Screen(tileEntity)
      case _ => null
    }
}