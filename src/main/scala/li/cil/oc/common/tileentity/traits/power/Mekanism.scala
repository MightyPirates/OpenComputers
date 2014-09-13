package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import li.cil.oc.Settings
import li.cil.oc.util.mods.Mods
import net.minecraftforge.common.ForgeDirection

trait Mekanism extends Common {
  @Optional.Method(modid = Mods.IDs.Mekanism)
  def canReceiveEnergy(side: ForgeDirection) = Mods.Mekanism.isAvailable && canConnectPower(side)

  @Optional.Method(modid = Mods.IDs.Mekanism)
  def transferEnergyToAcceptor(side: ForgeDirection, amount: Double) =
    if (!Mods.Mekanism.isAvailable) 0
    else tryChangeBuffer(side, amount * Settings.ratioMekanism) / Settings.ratioMekanism

  @Optional.Method(modid = Mods.IDs.Mekanism)
  def getMaxEnergy = ForgeDirection.VALID_DIRECTIONS.map(globalBufferSize).max / Settings.ratioMekanism

  @Optional.Method(modid = Mods.IDs.Mekanism)
  def getEnergy = ForgeDirection.VALID_DIRECTIONS.map(globalBuffer).max / Settings.ratioMekanism

  @Optional.Method(modid = Mods.IDs.Mekanism)
  def setEnergy(energy: Double) {}
}
