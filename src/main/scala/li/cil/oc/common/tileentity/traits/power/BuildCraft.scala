package li.cil.oc.common.tileentity.traits.power

import buildcraft.api.power.{IPowerReceptor, PowerHandler}
import cpw.mods.fml.common.Optional
import li.cil.oc.Settings
import li.cil.oc.util.mods.Mods
import net.minecraftforge.common.ForgeDirection

@Optional.Interface(iface = "buildcraft.api.power.IPowerReceptor", modid = Mods.IDs.BuildCraftPower)
trait BuildCraft extends Common with IPowerReceptor {
  private var powerHandler: Option[AnyRef] = None

  private lazy val useBuildCraftPower = isServer && !Settings.get.ignorePower && Mods.BuildCraftPower.isAvailable

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (useBuildCraftPower && world.getWorldTime % Settings.get.tickFrequency == 0) {
      for (side <- ForgeDirection.VALID_DIRECTIONS) {
        val demand = (globalBufferSize(side) - globalBuffer(side)) / Settings.ratioBC
        if (demand > 1) {
          val power = getPowerProvider.useEnergy(1, demand.toFloat, true)
          tryChangeBuffer(side, power * Settings.ratioBC)
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = Mods.IDs.BuildCraftPower)
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

  @Optional.Method(modid = Mods.IDs.BuildCraftPower)
  def getPowerReceiver(side: ForgeDirection) =
    if (canConnectPower(side))
      getPowerProvider.getPowerReceiver
    else null

  // Don't strip, also defined by AbstractBusAware trait.
  def getWorld = getWorldObj

  @Optional.Method(modid = Mods.IDs.BuildCraftPower)
  def doWork(workProvider: PowerHandler) {}
}
