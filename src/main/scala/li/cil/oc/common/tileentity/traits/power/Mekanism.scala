package li.cil.oc.common.tileentity.traits.power
/* TODO Mekanism

import cpw.mods.fml.common.Optional
import li.cil.oc.common.asm.Injectable
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.Power
import net.minecraftforge.common.util.ForgeDirection

@Injectable.Interface(value = "mekanism.api.energy.IStrictEnergyAcceptor", modid = Mods.IDs.Mekanism)
trait Mekanism extends Common {
  @Optional.Method(modid = Mods.IDs.Mekanism)
  def canReceiveEnergy(side: ForgeDirection) = Mods.Mekanism.isAvailable && canConnectPower(side)

  @Optional.Method(modid = Mods.IDs.Mekanism)
  def transferEnergyToAcceptor(side: ForgeDirection, amount: Double) =
    if (!Mods.Mekanism.isAvailable) 0
    else Power.toJoules(tryChangeBuffer(side, Power.fromJoules(amount)))

  @Optional.Method(modid = Mods.IDs.Mekanism)
  def getMaxEnergy = Power.toJoules(ForgeDirection.VALID_DIRECTIONS.map(globalBufferSize).max)

  @Optional.Method(modid = Mods.IDs.Mekanism)
  def getEnergy = Power.toJoules(ForgeDirection.VALID_DIRECTIONS.map(globalBuffer).max)

  @Optional.Method(modid = Mods.IDs.Mekanism)
  def setEnergy(energy: Double) {}
}
*/