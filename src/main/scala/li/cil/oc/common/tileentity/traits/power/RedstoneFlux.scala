package li.cil.oc.common.tileentity.traits.power

import li.cil.oc.common.asm.Injectable
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.Power
import net.minecraft.util.EnumFacing
import net.minecraftforge.fml.common.Optional

@Injectable.Interface(value = "cofh.api.energy.IEnergyReceiver", modid = Mods.IDs.CoFHEnergy)
trait RedstoneFlux extends Common {
  // IEnergyReceiver

  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def receiveEnergy(from: EnumFacing, maxReceive: Int, simulate: Boolean): Int =
    if (!Mods.CoFHEnergy.isModAvailable) 0
    else Power.toRF(tryChangeBuffer(from, Power.fromRF(maxReceive), !simulate))

  // IEnergyHandler

  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def getEnergyStored(from: EnumFacing): Int = Power.toRF(globalBuffer(from))

  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def getMaxEnergyStored(from: EnumFacing): Int = Power.toRF(globalBufferSize(from))

  // IEnergyConnection

  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def canConnectEnergy(from: EnumFacing): Boolean = Mods.CoFHEnergy.isModAvailable && canConnectPower(from)
}
