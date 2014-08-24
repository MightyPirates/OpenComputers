package li.cil.oc.common.tileentity.traits.power

import buildcraft.api.power.{IPowerReceptor, PowerHandler}
import cpw.mods.fml.common.Optional
import li.cil.oc.util.mods.Mods
import li.cil.oc.{OpenComputers, Settings}
import net.minecraftforge.common.util.ForgeDirection

trait BuildCraft extends Common {
  private lazy val useBuildCraftPower = isServer && Mods.BuildCraftPower.isAvailable

  private var powerHandler: Option[AnyRef] = None

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (useBuildCraftPower && world.getTotalWorldTime % Settings.get.tickFrequency == 0) {
      for (side <- ForgeDirection.VALID_DIRECTIONS) {
        val demand = (globalBufferSize(side) - globalBuffer(side)) / Settings.ratioBuildCraft
        if (demand > 1) {
          val power = getPowerProvider.useEnergy(1, demand.toFloat, true)
          tryChangeBuffer(side, power * Settings.ratioBuildCraft)
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = Mods.IDs.BuildCraftPower)
  def getPowerProvider = {
    if (Mods.BuildCraftPower.isAvailable && powerHandler.isEmpty) {
      this match {
        case receptor: IPowerReceptor =>
          val handler = new PowerHandler(receptor, PowerHandler.Type.MACHINE)
          if (handler != null) {
            handler.configure(1, 320, Float.MaxValue, 640)
            handler.configurePowerPerdition(0, 0)
            powerHandler = Some(handler)
          }
        case _ =>
          OpenComputers.log.warn("Failed setting up BuildCraft power, which most likely means the class transformer did not run. You're probably running in an incorrectly configured development environment. Try adding `-Dfml.coreMods.load=li.cil.oc.common.launch.TransformerLoader` to the VM options of your run configuration.")
      }
    }
    if (powerHandler.isDefined)
      powerHandler.get.asInstanceOf[PowerHandler]
    else null
  }

  @Optional.Method(modid = Mods.IDs.BuildCraftPower)
  def getPowerReceiver(side: ForgeDirection) =
    if (Mods.BuildCraftPower.isAvailable && canConnectPower(side))
      getPowerProvider.getPowerReceiver
    else null

  // Don't strip, also defined by AbstractBusAware trait.
  def getWorld = getWorldObj

  @Optional.Method(modid = Mods.IDs.BuildCraftPower)
  def doWork(workProvider: PowerHandler) {}
}
