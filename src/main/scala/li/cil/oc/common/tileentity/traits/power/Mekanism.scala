package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import li.cil.oc.Settings
import li.cil.oc.util.mods.Mods
import mekanism.api.energy.IStrictEnergyAcceptor
import net.minecraftforge.common.ForgeDirection

@Optional.Interface(iface = "mekanism.api.energy.IStrictEnergyAcceptor", modid = Mods.IDs.Mekanism)
trait Mekanism extends Common with IStrictEnergyAcceptor {
  @Optional.Method(modid = Mods.IDs.Mekanism)
  override def canReceiveEnergy(side: ForgeDirection) = canConnectPower(side)

  @Optional.Method(modid = Mods.IDs.Mekanism)
  override def transferEnergyToAcceptor(side: ForgeDirection, amount: Double) = tryChangeBuffer(side, amount * Settings.ratioMekanism) / Settings.ratioMekanism

  @Optional.Method(modid = Mods.IDs.Mekanism)
  override def getMaxEnergy = ForgeDirection.VALID_DIRECTIONS.map(globalBufferSize).max / Settings.ratioMekanism

  @Optional.Method(modid = Mods.IDs.Mekanism)
  override def getEnergy = ForgeDirection.VALID_DIRECTIONS.map(globalBuffer).max / Settings.ratioMekanism

  @Optional.Method(modid = Mods.IDs.Mekanism)
  override def setEnergy(energy: Double) {}
}
