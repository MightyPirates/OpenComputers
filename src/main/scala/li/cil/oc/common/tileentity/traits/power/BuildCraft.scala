package li.cil.oc.common.tileentity.traits.power

import buildcraft.api.power.{IPowerReceptor, PowerHandler}
import cpw.mods.fml.common.Optional
import li.cil.oc.Settings
import li.cil.oc.util.mods.Mods
import net.minecraftforge.common.ForgeDirection

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
      val handler = new PowerHandler(this.asInstanceOf[IPowerReceptor], PowerHandler.Type.MACHINE)
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
    if (Mods.BuildCraftPower.isAvailable && canConnectPower(side))
      getPowerProvider.getPowerReceiver
    else null

  // Don't strip, also defined by AbstractBusAware trait.
  def getWorld = getWorldObj

  @Optional.Method(modid = Mods.IDs.BuildCraftPower)
  def doWork(workProvider: PowerHandler) {}
}
