package li.cil.oc.common.gui

import cpw.mods.fml.common.network.IGuiHandler
import li.cil.oc.common.container.ContainerComputer
import li.cil.oc.common.tileentity.TileEntityComputer
import li.cil.oc.common.tileentity.TileEntityScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World

object GuiType extends Enumeration {
  val Computer = Value("Computer")
  val Screen = Value("Screen")
}

class GuiHandler extends IGuiHandler {
  override def getServerGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case tileEntity: TileEntityComputer =>
        new ContainerComputer(player.inventory, tileEntity)
      case _ => null
    }

  override def getClientGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case tileEntity: TileEntityComputer if id == GuiType.Computer.id =>
        new GuiComputer(player.inventory, tileEntity)
      case tileEntity: TileEntityScreen if id == GuiType.Screen.id =>
        new GuiScreen(tileEntity)
      case _ => null
    }
}