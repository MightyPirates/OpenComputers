package li.cil.oc.integration.computercraft

import dan200.computercraft.api.ComputerCraftAPI
import dan200.computercraft.api.peripheral.IPeripheral
import dan200.computercraft.api.peripheral.IPeripheralProvider
import li.cil.oc.common.tileentity.traits.SwitchLike
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.world.World

object PeripheralProvider extends IPeripheralProvider {
  def init() {
    ComputerCraftAPI.registerPeripheralProvider(this)
  }

  override def getPeripheral(world: World, blockPos: BlockPos, enumFacing: EnumFacing): IPeripheral = world.getTileEntity(blockPos) match {
    case switch: SwitchLike => new SwitchPeripheral(switch)
    case _ => null
  }
}
