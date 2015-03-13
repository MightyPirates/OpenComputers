package li.cil.oc.common.tileentity.traits.power

import li.cil.oc.Settings
import li.cil.oc.common.asm.Injectable
import li.cil.oc.integration.Mods
import net.minecraft.util.EnumFacing
import net.minecraftforge.fml.common.Optional

@Injectable.Interface(value = "cofh.api.energy.IEnergyReceiver", modid = Mods.IDs.CoFHEnergy)
trait RedstoneFlux extends Common {
  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def canConnectEnergy(from: EnumFacing) = Mods.CoFHEnergy.isAvailable && canConnectPower(from)

  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def receiveEnergy(from: EnumFacing, maxReceive: Int, simulate: Boolean) =
    if (!Mods.CoFHEnergy.isAvailable) 0
    else (tryChangeBuffer(from, maxReceive * Settings.get.ratioRedstoneFlux, !simulate) / Settings.get.ratioRedstoneFlux).toInt

  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def getEnergyStored(from: EnumFacing) = (globalBuffer(from) / Settings.get.ratioRedstoneFlux).toInt

  @Optional.Method(modid = Mods.IDs.CoFHEnergy)
  def getMaxEnergyStored(from: EnumFacing) = (globalBufferSize(from) / Settings.get.ratioRedstoneFlux).toInt
}
