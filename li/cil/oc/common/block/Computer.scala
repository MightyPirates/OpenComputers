package li.cil.oc.common.block

import li.cil.oc.OpenComputers
import li.cil.oc.common.{GuiType, tileentity}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.{World, IBlockAccess}
import net.minecraftforge.common.ForgeDirection

abstract class Computer extends Delegate {
  override def hasTileEntity = true

  override def canConnectRedstone(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) =
    world.getBlockTileEntity(x, y, z).asInstanceOf[tileentity.Computer].isOutputEnabled

  override def isProvidingStrongPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) =
    isProvidingWeakPower(world, x, y, z, side)

  override def isProvidingWeakPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) =
    world.getBlockTileEntity(x, y, z).asInstanceOf[tileentity.Computer].output(side)

  override def onBlockPreDestroy(world: World, x: Int, y: Int, z: Int) =
    if (!world.isRemote) world.getBlockTileEntity(x, y, z) match {
      case computer: tileentity.Computer =>
        computer.instance.stop()
        computer.dropContent(world, x, y, z)
      case _ => // Ignore.
    }

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
                                side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking) {
      // Start the computer if it isn't already running and open the GUI.
      if (!world.isRemote) {
        world.getBlockTileEntity(x, y, z).asInstanceOf[tileentity.Computer].instance.start()
      }
      player.openGui(OpenComputers, GuiType.Case.id, world, x, y, z)
      true
    }
    else false
  }

  override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, blockId: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case computer: tileentity.Computer => computer.checkRedstoneInputChanged()
      case _ => // Ignore.
    }
}
