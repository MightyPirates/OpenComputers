package li.cil.oc.common.tileentity.traits.power

import buildcraft.api.power.{PowerHandler, IPowerReceptor}
import cpw.mods.fml.common.Optional
import li.cil.oc.Settings
import net.minecraftforge.common.util.ForgeDirection

@Optional.Interface(iface = "buildcraft.api.power.IPowerReceptor", modid = "BuildCraftAPI|power")
trait BuildCraft extends UniversalElectricity with IPowerReceptor {
  private var powerHandler: Option[AnyRef] = None

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = "BuildCraftAPI|power")
  override def updateEntity() {
    super.updateEntity()
    if (isServer && !Settings.get.ignorePower) {
      if (world.getWorldTime % Settings.get.tickFrequency == 0) {
        for (side <- ForgeDirection.VALID_DIRECTIONS) connector(side) match {
          case Some(node) =>
            val demand = node.globalBufferSize - node.globalBuffer
            if (demand > 1) {
              node.changeBuffer(getPowerProvider.useEnergy(1, demand.toFloat, true))
            }
          case _ =>
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = "BuildCraftAPI|power")
  def getPowerProvider = {
    if (powerHandler.isEmpty) {
      val handler = new PowerHandler(this, PowerHandler.Type.MACHINE)
      if (handler != null) {
        handler.configure(1, 320, Float.MaxValue, 640)
        handler.configurePowerPerdition(0, 0)
        powerHandler = Some(handler)
      }
    }
    if (powerHandler.isDefined) powerHandler.get.asInstanceOf[PowerHandler]
    else null
  }

  @Optional.Method(modid = "BuildCraftAPI|power")
  def getPowerReceiver(side: ForgeDirection) =
    if (!Settings.get.ignorePower && (if (isClient) hasConnector(side) else connector(side).isDefined))
      getPowerProvider.getPowerReceiver
    else null

  // Don't strip, also defined by AbstractBusAware trait.
  def getWorld = getWorldObj

  @Optional.Method(modid = "BuildCraftAPI|power")
  def doWork(workProvider: PowerHandler) {}
}
