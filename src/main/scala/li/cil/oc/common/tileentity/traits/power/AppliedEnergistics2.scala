package li.cil.oc.common.tileentity.traits.power
import java.util

import appeng.api._
import appeng.api.config.Actionable
import appeng.api.config.PowerMultiplier
import appeng.api.networking._
import appeng.api.networking.energy.IEnergyGrid
import appeng.api.util.{AECableType, AEColor, AEPartLocation, DimensionalCoord}
import li.cil.oc.Settings
import li.cil.oc.common.EventHandler
import li.cil.oc.integration.Mods
import li.cil.oc.integration.appeng.AEUtil
import li.cil.oc.integration.util.Power
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.common._

import scala.collection.JavaConversions

trait AppliedEnergistics2 extends Common with IGridHost {
  private lazy val useAppliedEnergistics2Power = isServer && Mods.AppliedEnergistics2.isModAvailable

  // 'Manual' lazy val, because lazy vals mess up the class loader, leading to class not found exceptions.
  private var node: Option[AnyRef] = None

  override def updateEntity() {
    super.updateEntity()
    if (useAppliedEnergistics2Power && getLevel.getGameTime % Settings.get.tickFrequency == 0) {
      updateEnergy()
    }
  }

  private def updateEnergy() {
    tryAllSides((demand, _) => {
      val grid = getGridNode(AEPartLocation.INTERNAL).getGrid
      if (grid != null) {
        val cache = grid.getCache(classOf[IEnergyGrid]).asInstanceOf[IEnergyGrid]
        if (cache != null) {
          cache.extractAEPower(demand, Actionable.MODULATE, PowerMultiplier.CONFIG)
        }
        else 0.0
      }
      else 0.0
    }, Power.fromAE, Power.toAE)
  }

  override def clearRemoved() {
    super.clearRemoved()
    if (useAppliedEnergistics2Power) EventHandler.scheduleAE2Add(this)
  }

  override def setRemoved() {
    super.setRemoved()
    if (useAppliedEnergistics2Power) securityBreak()
  }

  override def onChunkUnloaded() {
    super.onChunkUnloaded()
    if (useAppliedEnergistics2Power) securityBreak()
  }

  // ----------------------------------------------------------------------- //

  override def loadForServer(nbt: CompoundNBT) {
    super.loadForServer(nbt)
    if (useAppliedEnergistics2Power) loadNode(nbt)
  }

  private def loadNode(nbt: CompoundNBT): Unit = {
    getGridNode(AEPartLocation.INTERNAL).loadFromNBT(Settings.namespace + "ae2power", nbt)
  }

  override def setLevelAndPosition(worldIn: World, pos: BlockPos): Unit = {
    if (getLevel == worldIn)
      return
    super.setLevelAndPosition(worldIn, pos)
    if (worldIn != null && isServer && useAppliedEnergistics2Power) {
      val gridNode = getGridNode(AEPartLocation.INTERNAL)
      if (gridNode != null) {
        gridNode.updateState()
      }
    }
  }

  override def saveForServer(nbt: CompoundNBT) {
    super.saveForServer(nbt)
    if (useAppliedEnergistics2Power) saveNode(nbt)
  }

  private def saveNode(nbt: CompoundNBT): Unit = {
    getGridNode(AEPartLocation.INTERNAL).saveToNBT(Settings.namespace + "ae2power", nbt)
  }

  // ----------------------------------------------------------------------- //

  def getGridNode(side: AEPartLocation): IGridNode = node match {
    case Some(gridNode: IGridNode) => gridNode
    case _ if isServer =>
      val gridNode = AEUtil.aeApi.get.grid.createGridNode(new AppliedEnergistics2GridBlock(this))
      node = Option(gridNode)
      gridNode
    case _ => null
  }

  def getCableConnectionType(side: AEPartLocation): AECableType = AECableType.SMART

  def securityBreak() {
    getGridNode(AEPartLocation.INTERNAL).destroy()
  }
}

class AppliedEnergistics2GridBlock(val tileEntity: AppliedEnergistics2) extends IGridBlock {
  override def getIdlePowerUsage: Double = 0.0

  override def getFlags: util.EnumSet[GridFlags] = util.EnumSet.noneOf(classOf[GridFlags])

  def isWorldAccessible: Boolean = true

  override def getLocation: DimensionalCoord = new DimensionalCoord(tileEntity)

  override def getGridColor: AEColor = AEColor.TRANSPARENT

  override def onGridNotification(p1: GridNotification): Unit = {}

  override def getConnectableSides: util.EnumSet[Direction] = {
    val connectableSides = JavaConversions.asJavaCollection(Direction.values.filter(tileEntity.canConnectPower))
    if (connectableSides.isEmpty) {
      val s = util.EnumSet.copyOf(JavaConversions.asJavaCollection(Direction.values))
      s.clear()
      s
    }
    else
      util.EnumSet.copyOf(connectableSides)
  }

  override def getMachine: IGridHost = tileEntity.asInstanceOf[IGridHost]

  override def gridChanged(): Unit = {}

  override def getMachineRepresentation: ItemStack = ItemStack.EMPTY
}
