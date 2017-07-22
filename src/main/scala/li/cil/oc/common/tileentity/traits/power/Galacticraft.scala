package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import li.cil.oc.common.asm.Injectable
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.Power
import micdoodle8.mods.galacticraft.api.power.EnergySource
import micdoodle8.mods.galacticraft.api.transmission.NetworkType
import net.minecraftforge.common.util.ForgeDirection

import scala.language.implicitConversions

@Injectable.InterfaceList(Array(
  new Injectable.Interface(value = "micdoodle8.mods.galacticraft.api.power.IEnergyHandlerGC", modid = Mods.IDs.Galacticraft),
  new Injectable.Interface(value = "micdoodle8.mods.galacticraft.api.transmission.tile.IConnector", modid = Mods.IDs.Galacticraft)
))
trait Galacticraft extends Common {
  private implicit def toDirection(source: EnergySource): ForgeDirection = source match {
    case adjacent: EnergySource.EnergySourceAdjacent => adjacent.direction
    case _ => ForgeDirection.UNKNOWN
  }

  @Optional.Method(modid = Mods.IDs.Galacticraft)
  def nodeAvailable(from: EnergySource) = Mods.Galacticraft.isAvailable && canConnectPower(from)

  @Optional.Method(modid = Mods.IDs.Galacticraft)
  def receiveEnergyGC(from: EnergySource, amount: Float, simulate: Boolean) =
    if (!Mods.Galacticraft.isAvailable) 0f
    else Power.toGC(tryChangeBuffer(from, Power.fromGC(amount), !simulate))

  @Optional.Method(modid = Mods.IDs.Galacticraft)
  def getEnergyStoredGC(from: EnergySource) = Power.toGC(globalBuffer(from))

  @Optional.Method(modid = Mods.IDs.Galacticraft)
  def getMaxEnergyStoredGC(from: EnergySource) = Power.toGC(globalBufferSize(from))

  @Optional.Method(modid = Mods.IDs.Galacticraft)
  def extractEnergyGC(from: EnergySource, amount: Float, simulate: Boolean) = 0f

  @Optional.Method(modid = Mods.IDs.Galacticraft)
  def canConnect(from: ForgeDirection, networkType: NetworkType): Boolean = networkType == NetworkType.POWER && canConnectPower(from)
}
