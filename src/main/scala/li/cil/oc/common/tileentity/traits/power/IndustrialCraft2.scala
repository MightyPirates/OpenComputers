package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import ic2.api.energy.tile.IEnergySink
import li.cil.oc.server.TickHandler
import net.minecraftforge.common.ForgeDirection
import li.cil.oc.Settings

@Optional.Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "IC2")
trait IndustrialCraft2 extends UniversalElectricity with IEnergySink {
  var addedToPowerGrid = false

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = "IC2")
  override def validate() {
    super.validate()
    if (!addedToPowerGrid) TickHandler.scheduleIC2Add(this)
  }

  @Optional.Method(modid = "IC2")
  override def invalidate() {
    super.invalidate()
    if (addedToPowerGrid) TickHandler.scheduleIC2Remove(this)
  }

  @Optional.Method(modid = "IC2")
  override def onChunkUnload() {
    super.onChunkUnload()
    if (addedToPowerGrid) TickHandler.scheduleIC2Remove(this)
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = "IC2")
  def getMaxSafeInput = Integer.MAX_VALUE

  @Optional.Method(modid = "IC2")
  def acceptsEnergyFrom(emitter: net.minecraft.tileentity.TileEntity, direction: ForgeDirection) = canConnect(direction, null)

  @Optional.Method(modid = "IC2")
  def demandedEnergyUnits = {
    if (Settings.get.ignorePower || isClient) 0
    else {
      var maxDemand = 0.0
      for (side <- ForgeDirection.VALID_DIRECTIONS) connector(side) match {
        case Some(node) =>
          maxDemand = math.max(maxDemand, node.globalBufferSize - node.globalBuffer)
        case _ =>
      }
      maxDemand * Settings.ratioBC / Settings.ratioIC2
    }
  }

  @Optional.Method(modid = "IC2")
  def injectEnergyUnits(directionFrom: ForgeDirection, amount: Double) =
    amount - onReceiveEnergy(directionFrom, (amount * Settings.ratioIC2).toLong, doReceive = true) / Settings.ratioIC2
}
