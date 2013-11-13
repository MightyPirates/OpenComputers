package li.cil.oc.common

import cpw.mods.fml.common.network.IGuiHandler
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World

abstract class GuiHandler extends IGuiHandler {
  override def getServerGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case computer: tileentity.Case =>
        new container.Case(player.inventory, computer)
      case drive: tileentity.DiskDrive =>
        new container.DiskDrive(player.inventory, drive)
      case _ => null
    }
}