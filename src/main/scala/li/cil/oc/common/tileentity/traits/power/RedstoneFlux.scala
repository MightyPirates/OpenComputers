package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import li.cil.oc.common.asm.Injectable
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.Power
import net.minecraftforge.common.util.ForgeDirection

@Injectable.Interface(value = "cofh.api.energy.IEnergyHandler", modid = Mods.IDs.CoFHEnergy)
trait RedstoneFlux extends Common {
  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def canConnectEnergy(from: ForgeDirection) = Mods.CoFHEnergy.isAvailable && canConnectPower(from)

  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def receiveEnergy(from: ForgeDirection, maxReceive: Int, simulate: Boolean) =
    if (!Mods.CoFHEnergy.isAvailable) 0
    else Power.toRF(tryChangeBuffer(from, Power.fromRF(maxReceive), !simulate))

  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def getEnergyStored(from: ForgeDirection) = Power.toRF(globalBuffer(from))

  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def getMaxEnergyStored(from: ForgeDirection) = Power.toRF(globalBufferSize(from))

  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def extractEnergy(from: ForgeDirection, maxExtract: Int, simulate: Boolean) = 0
}
