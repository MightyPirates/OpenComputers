package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import factorization.api.{Charge, Coord, IChargeConductor}
import li.cil.oc.Settings
import li.cil.oc.util.mods.Mods
import net.minecraftforge.common.ForgeDirection

@Optional.Interface(iface = "factorization.api.IChargeConductor", modid = Mods.IDs.Factorization)
trait Factorization extends Common with IChargeConductor {
  private lazy val charge: AnyRef = new Charge(this)

  private lazy val useFactorizationPower = isServer && !Settings.get.ignorePower && Mods.Factorization.isAvailable

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    if (useFactorizationPower) {
      getCharge.update()
      for (side <- ForgeDirection.VALID_DIRECTIONS) {
        val demand = (globalBufferSize(side) - globalBuffer(side)) / Settings.ratioFactorization
        if (demand > 1) {
          val power = getCharge.deplete(demand.toInt)
          tryChangeBuffer(side, power * Settings.ratioFactorization)
        }
      }
    }
    super.updateEntity()
  }

  override def invalidate() {
    if (useFactorizationPower) {
      getCharge.invalidate()
    }
    super.invalidate()
  }

  override def onChunkUnload() {
    if (useFactorizationPower && !isInvalid) {
      getCharge.remove()
    }
    super.onChunkUnload()
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = Mods.IDs.Factorization)
  override def getCharge = charge.asInstanceOf[Charge]

  @Optional.Method(modid = Mods.IDs.Factorization)
  override def getInfo = ""

  @Optional.Method(modid = Mods.IDs.Factorization)
  override def getCoord = new Coord(this)
}
