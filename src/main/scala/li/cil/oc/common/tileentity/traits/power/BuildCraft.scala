package li.cil.oc.common.tileentity.traits.power

import buildcraft.api.power.IPowerReceptor
import buildcraft.api.power.PowerHandler
import cpw.mods.fml.common.Optional
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.mods.Mods
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

trait BuildCraft extends Common {
  private lazy val useBuildCraftPower = isServer && Mods.BuildCraftPower.isAvailable

  private var powerHandler: Option[AnyRef] = None

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (useBuildCraftPower && world.getTotalWorldTime % Settings.get.tickFrequency == 0) {
      updateEnergy()
    }
  }

  @Optional.Method(modid = Mods.IDs.BuildCraftPower)
  private def updateEnergy() {
    tryAllSides((demand, _) => getPowerProvider.useEnergy(1, demand.toFloat, true), Settings.get.ratioBuildCraft)
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (useBuildCraftPower) loadHandler(nbt)
  }

  @Optional.Method(modid = Mods.IDs.BuildCraftPower)
  private def loadHandler(nbt: NBTTagCompound) {
    Option(getPowerProvider).foreach(_.readFromNBT(nbt.getCompoundTag(Settings.namespace + "bcpower")))
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    if (useBuildCraftPower) saveHandler(nbt)
  }

  @Optional.Method(modid = Mods.IDs.BuildCraftPower)
  private def saveHandler(nbt: NBTTagCompound) {
    Option(getPowerProvider).foreach(h => nbt.setNewCompoundTag(Settings.namespace + "bcpower", h.writeToNBT))
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = Mods.IDs.BuildCraftPower)
  def getPowerProvider = {
    if (Mods.BuildCraftPower.isAvailable && powerHandler.isEmpty) {
      this match {
        case receptor: IPowerReceptor =>
          val handler = new PowerHandler(receptor, PowerHandler.Type.MACHINE)
          if (handler != null) {
            val conversionBufferSize = energyThroughput * Settings.get.tickFrequency / Settings.get.ratioBuildCraft
            handler.configure(1, conversionBufferSize, Float.MaxValue, conversionBufferSize)
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
