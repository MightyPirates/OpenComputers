package li.cil.oc.integration.computercraft

import dan200.computercraft.api.ComputerCraftAPI
import dan200.computercraft.api.peripheral.IPeripheralProvider
import li.cil.oc.common.tileentity.traits.SwitchLike
import net.minecraft.world.World

object PeripheralProvider extends IPeripheralProvider {
  def init() {
    ComputerCraftAPI.registerPeripheralProvider(this)
  }

  override def getPeripheral(world: World, x: Int, y: Int, z: Int, side: Int) = world.getTileEntity(x, y, z) match {
    case switch: SwitchLike => new SwitchPeripheral(switch)
    case _ => null
  }
}
