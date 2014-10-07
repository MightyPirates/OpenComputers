package li.cil.oc.common.tileentity.traits.power

import java.util

import appeng.api.AEApi
import appeng.api.config.Actionable
import appeng.api.config.PowerMultiplier
import appeng.api.networking._
import appeng.api.networking.energy.IEnergyGrid
import appeng.api.util.AECableType
import appeng.api.util.AEColor
import appeng.api.util.DimensionalCoord
import cpw.mods.fml.common.Optional
import li.cil.oc.Settings
import li.cil.oc.common.EventHandler
import li.cil.oc.util.mods.Mods
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsJava._

trait AppliedEnergistics2 extends Common {
  private lazy val useAppliedEnergistics2Power = isServer && Mods.AppliedEnergistics2.isAvailable

  // 'Manual' lazy val, because lazy vals mess up the class loader, leading to class not found exceptions.
  private var node: Option[AnyRef] = None

  override def updateEntity() {
    super.updateEntity()
    if (useAppliedEnergistics2Power && world.getTotalWorldTime % Settings.get.tickFrequency == 0) {
      updateEnergy()
    }
  }

  @Optional.Method(modid = Mods.IDs.AppliedEnergistics2)
  private def updateEnergy() {
    tryAllSides((demand, side) => {
      val grid = getGridNode(side).getGrid
      if (grid != null) {
        val cache = grid.getCache(classOf[IEnergyGrid]).asInstanceOf[IEnergyGrid]
        if (cache != null) {
          cache.extractAEPower(demand, Actionable.MODULATE, PowerMultiplier.CONFIG)
        }
        else 0.0
      }
      else 0.0
    }, Settings.get.ratioAppliedEnergistics2)
  }

  override def validate() {
    super.validate()
    if (useAppliedEnergistics2Power) EventHandler.scheduleAE2Add(this)
  }

  override def invalidate() {
    super.invalidate()
    if (useAppliedEnergistics2Power) securityBreak()
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    if (useAppliedEnergistics2Power) securityBreak()
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (useAppliedEnergistics2Power) loadNode(nbt)
  }

  @Optional.Method(modid = Mods.IDs.AppliedEnergistics2)
  private def loadNode(nbt: NBTTagCompound): Unit = {
    getGridNode(ForgeDirection.UNKNOWN).loadFromNBT(Settings.namespace + "ae2power", nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    if (useAppliedEnergistics2Power) saveNode(nbt)
  }

  @Optional.Method(modid = Mods.IDs.AppliedEnergistics2)
  private def saveNode(nbt: NBTTagCompound): Unit = {
    getGridNode(ForgeDirection.UNKNOWN).saveToNBT(Settings.namespace + "ae2power", nbt)
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = Mods.IDs.AppliedEnergistics2)
  def getGridNode(side: ForgeDirection) = node match {
    case Some(gridNode: IGridNode) => gridNode
    case _ =>
      val gridNode = AEApi.instance.createGridNode(new AppliedEnergistics2GridBlock(this))
      node = Option(gridNode)
      gridNode
  }

  @Optional.Method(modid = Mods.IDs.AppliedEnergistics2)
  def getCableConnectionType(side: ForgeDirection) = AECableType.SMART

  @Optional.Method(modid = Mods.IDs.AppliedEnergistics2)
  def securityBreak() {
    getGridNode(ForgeDirection.UNKNOWN).destroy()
  }
}

class AppliedEnergistics2GridBlock(val tileEntity: AppliedEnergistics2) extends IGridBlock {
  override def getIdlePowerUsage = 0.0

  override def getFlags = util.EnumSet.noneOf(classOf[GridFlags])

  override def isWorldAccessible = true

  override def getLocation = new DimensionalCoord(tileEntity)

  override def getGridColor = AEColor.Transparent

  override def onGridNotification(p1: GridNotification) {}

  override def setNetworkStatus(p1: IGrid, p2: Int) {}

  override def getConnectableSides = util.EnumSet.copyOf(ForgeDirection.VALID_DIRECTIONS.filter(tileEntity.canConnectPower).toList)

  override def getMachine = tileEntity.asInstanceOf[IGridHost]

  override def gridChanged() {}

  override def getMachineRepresentation = null
}