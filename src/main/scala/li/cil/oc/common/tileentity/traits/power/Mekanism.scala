package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import li.cil.oc.Settings
import li.cil.oc.common.asm.Injectable
import li.cil.oc.integration.Mods
import net.minecraftforge.common.util.ForgeDirection

@Injectable.Interface(value = "mekanism.api.energy.IStrictEnergyAcceptor", modid = Mods.IDs.Mekanism)
trait Mekanism extends Common {
  @Optional.Method(modid = Mods.IDs.Mekanism)
  def canReceiveEnergy(side: ForgeDirection) = Mods.Mekanism.isAvailable && canConnectPower(side)

  @Optional.Method(modid = Mods.IDs.Mekanism)
  def transferEnergyToAcceptor(side: ForgeDirection, amount: Double) =
    if (!Mods.Mekanism.isAvailable) 0
    else amount - tryChangeBuffer(side, amount * Settings.get.ratioMekanism) / Settings.get.ratioMekanism

  @Optional.Method(modid = Mods.IDs.Mekanism)
  def getMaxEnergy = ForgeDirection.VALID_DIRECTIONS.map(globalBufferSize).max / Settings.get.ratioMekanism

  @Optional.Method(modid = Mods.IDs.Mekanism)
  def getEnergy = ForgeDirection.VALID_DIRECTIONS.map(globalBuffer).max / Settings.get.ratioMekanism

  @Optional.Method(modid = Mods.IDs.Mekanism)
  def setEnergy(energy: Double) {}
}
