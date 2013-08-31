package li.cil.oc.gui

import cpw.mods.fml.common.network.IGuiHandler
import net.minecraft.world.World
import net.minecraft.entity.player.EntityPlayer
import li.cil.oc.common.tileentity.TileEntityComputer
import li.cil.oc.container.ContainerComputer

class GuiHandler extends IGuiHandler {

  override def getServerGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Object = {
    var tileEntity = world.getBlockTileEntity(x, y, z);
    if (tileEntity.isInstanceOf[TileEntityComputer]) {
      return new ContainerComputer(player.inventory, tileEntity.asInstanceOf[TileEntityComputer]);
    }
    return null;
  }
  override def getClientGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Object = {
    var tileEntity = world.getBlockTileEntity(x, y, z);
    if (tileEntity.isInstanceOf[TileEntityComputer]) {
      return new GuiComputer(player.inventory,  tileEntity.asInstanceOf[TileEntityComputer]);
    }
    return null;
  }
}