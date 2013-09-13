package li.cil.oc.common.gui

import cpw.mods.fml.common.network.IGuiHandler
import li.cil.oc.common.container.ContainerComputer
import li.cil.oc.common.tileentity.TileEntityComputer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import li.cil.oc.common.tileentity.TileEntityScreen

class GuiHandler extends IGuiHandler {
  override def getServerGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case tileEntity: TileEntityComputer =>
        new ContainerComputer(player.inventory, tileEntity)
      case _ => null
    }

  override def getClientGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case tileEntity: TileEntityComputer =>
        new GuiComputer(player.inventory, tileEntity)
      case tileEntity:TileEntityScreen =>
        new ScreenGui(tileEntity)
      case _ => null
    }
}