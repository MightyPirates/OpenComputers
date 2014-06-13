package li.cil.oc.common.tileentity.traits.power

import buildcraft.api.power.{PowerHandler, IPowerReceptor}
import cpw.mods.fml.common.{ModAPIManager, Optional}
import li.cil.oc.Settings
import net.minecraftforge.common.util.ForgeDirection

@Optional.Interface(iface = "buildcraft.api.power.IPowerReceptor", modid = "BuildCraftAPI|power")
trait BuildCraft extends Common with IPowerReceptor {
  private var powerHandler: Option[AnyRef] = None

  private lazy val useBuildCraftPower = isServer && !Settings.get.ignorePower && ModAPIManager.INSTANCE.hasAPI("BuildCraftAPI|power")

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (useBuildCraftPower && world.getWorldTime % Settings.get.tickFrequency == 0) {
      for (side <- ForgeDirection.VALID_DIRECTIONS) {
        val demand = globalBufferSize(side) - globalBuffer(side)
        if (demand > 1) {
          val power = getPowerProvider.useEnergy(1, demand.toFloat, true)
          tryChangeBuffer(side, power * Settings.ratioBC)
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
    if (powerHandler.isDefined)
      powerHandler.get.asInstanceOf[PowerHandler]
    else null
  }

  @Optional.Method(modid = "BuildCraftAPI|power")
  def getPowerReceiver(side: ForgeDirection) =
    if (canConnectPower(side))
      getPowerProvider.getPowerReceiver
    else null

  // Don't strip, also defined by AbstractBusAware trait.
  def getWorld = getWorldObj

  @Optional.Method(modid = "BuildCraftAPI|power")
  def doWork(workProvider: PowerHandler) {}
}
