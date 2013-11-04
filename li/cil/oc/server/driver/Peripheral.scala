package li.cil.oc.server.driver

import dan200.computer.api.IPeripheral
import li.cil.oc.api.driver
import li.cil.oc.server.component
import net.minecraft.world.World

object Peripheral extends driver.Block {
  def worksWith(world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case peripheral: IPeripheral => true
      case _ => false
    }

  def createEnvironment(world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case peripheral: IPeripheral => new component.Peripheral(peripheral)
      case _ => null
    }
}
