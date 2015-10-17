package li.cil.oc.common.tileentity.traits.power

import li.cil.oc.common.asm.Injectable
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.Power
import net.minecraft.util.EnumFacing
import net.minecraftforge.fml.common.Optional

@Injectable.Interface(value = "cofh.api.energy.IEnergyReceiver", modid = Mods.IDs.CoFHEnergy)
trait RedstoneFlux extends Common {
  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def canConnectEnergy(from: EnumFacing) = Mods.CoFHEnergy.isAvailable && canConnectPower(from)

  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def receiveEnergy(from: EnumFacing, maxReceive: Int, simulate: Boolean) =
    if (!Mods.CoFHEnergy.isAvailable) 0
    else Power.toRF(tryChangeBuffer(from, Power.fromRF(maxReceive), !simulate))

  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def getEnergyStored(from: EnumFacing) = Power.toRF(globalBuffer(from))

  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def getMaxEnergyStored(from: EnumFacing) = Power.toRF(globalBufferSize(from))

  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def extractEnergy(from: EnumFacing, maxExtract: Int, simulate: Boolean) = 0
}
