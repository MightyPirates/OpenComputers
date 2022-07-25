package li.cil.oc.common.tileentity

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.common.template.AssemblerTemplates
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.StackOption
import li.cil.oc.util.StackOption._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

import scala.collection.convert.ImplicitConversionsToJava._

class Assembler extends TileEntity(null) with traits.Environment with traits.PowerAcceptor with traits.Inventory with SidedEnvironment with traits.StateAware with traits.Tickable with DeviceInfo {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("assembler").
    withConnector(Settings.get.bufferConverter).
    create()

  var output: StackOption = EmptyStack

  var totalRequiredEnergy = 0.0

  var requiredEnergy = 0.0

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "Assembler",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Factorizer R1D1"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  @OnlyIn(Dist.CLIENT)
  override def canConnect(side: Direction) = side != Direction.UP

  override def sidedNode(side: Direction) = if (side != Direction.UP) node else null

  @OnlyIn(Dist.CLIENT)
  override protected def hasConnector(side: Direction) = canConnect(side)

  override protected def connector(side: Direction) = Option(if (side != Direction.UP) node else null)

  override def energyThroughput = Settings.get.assemblerRate

  override def getCurrentState = {
    if (isAssembling) util.EnumSet.of(api.util.StateAware.State.IsWorking)
    else if (canAssemble) util.EnumSet.of(api.util.StateAware.State.CanWork)
    else util.EnumSet.noneOf(classOf[api.util.StateAware.State])
  }

  // ----------------------------------------------------------------------- //

  def canAssemble = AssemblerTemplates.select(getItem(0)) match {
    case Some(template) => !isAssembling && output.isEmpty && template.validate(this)._1
    case _ => false
  }

  def isAssembling = requiredEnergy > 0

  def progress = (1 - requiredEnergy / totalRequiredEnergy) * 100

  def timeRemaining = (requiredEnergy / Settings.get.assemblerTickAmount / 20).toInt

  // ----------------------------------------------------------------------- //

  def start(finishImmediately: Boolean = false): Boolean = this.synchronized {
    AssemblerTemplates.select(getItem(0)) match {
      case Some(template) if !isAssembling && output.isEmpty && template.validate(this)._1 =>
        for (slot <- 0 until getContainerSize) {
          val stack = getItem(slot)
          if (!stack.isEmpty && !canPlaceItem(slot, stack)) return false
        }
        val (stack, energy) = template.assemble(this)
        output = StackOption(stack)
        if (finishImmediately) {
          totalRequiredEnergy = 0
        }
        else {
          totalRequiredEnergy = math.max(1, energy)
        }
        requiredEnergy = totalRequiredEnergy
        ServerPacketSender.sendRobotAssembling(this, assembling = true)

        for (slot <- 0 until getContainerSize) updateItems(slot, ItemStack.EMPTY)
        setChanged()

        true
      case _ => false
    }
  }

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function(): string, number or boolean -- The current state of the assembler, `busy' or `idle', followed by the progress or template validity, respectively.""")
  def status(context: Context, args: Arguments): Array[Object] = {
    if (isAssembling) result("busy", progress)
    else AssemblerTemplates.select(getItem(0)) match {
      case Some(template) if template.validate(this)._1 => result("idle", true)
      case _ => result("idle", false)
    }
  }

  @Callback(doc = """function():boolean -- Start assembling, if possible. Returns whether assembly was started or not.""")
  def start(context: Context, args: Arguments): Array[Object] = result(start())

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (!output.isEmpty && getLevel.getGameTime % Settings.get.tickFrequency == 0) {
      val want = math.max(1, math.min(requiredEnergy, Settings.get.assemblerTickAmount * Settings.get.tickFrequency))
      val have = want + (if (Settings.get.ignorePower) 0 else node.changeBuffer(-want))
      requiredEnergy -= have
      if (requiredEnergy <= 0) {
        setItem(0, output.get)
        output = EmptyStack
        requiredEnergy = 0
      }
      ServerPacketSender.sendRobotAssembling(this, have > 0.5 && !output.isEmpty)
    }
  }

  // ----------------------------------------------------------------------- //

  private final val OutputTag = Settings.namespace + "output"
  private final val OutputTagCompat = Settings.namespace + "robot"
  private final val TotalTag = Settings.namespace + "total"
  private final val RemainingTag = Settings.namespace + "remaining"

  override def loadForServer(nbt: CompoundNBT) {
    super.loadForServer(nbt)
    if (nbt.contains(OutputTag)) {
      output = StackOption(ItemStack.of(nbt.getCompound(OutputTag)))
    }
    else if (nbt.contains(OutputTagCompat)) {
      output = StackOption(ItemStack.of(nbt.getCompound(OutputTagCompat)))
    }
    totalRequiredEnergy = nbt.getDouble(TotalTag)
    requiredEnergy = nbt.getDouble(RemainingTag)
  }

  override def saveForServer(nbt: CompoundNBT) {
    super.saveForServer(nbt)
    nbt.setNewCompoundTag(OutputTag, output.get.save)
    nbt.putDouble(TotalTag, totalRequiredEnergy)
    nbt.putDouble(RemainingTag, requiredEnergy)
  }

  @OnlyIn(Dist.CLIENT) override
  def loadForClient(nbt: CompoundNBT) {
    super.loadForClient(nbt)
    requiredEnergy = nbt.getDouble(RemainingTag)
  }

  override def saveForClient(nbt: CompoundNBT) {
    super.saveForClient(nbt)
    nbt.putDouble(RemainingTag, requiredEnergy)
  }

  // ----------------------------------------------------------------------- //

  override def getContainerSize = 22

  override def getMaxStackSize = 1

  override def canPlaceItem(slot: Int, stack: ItemStack) =
    if (slot == 0) {
      !isAssembling && AssemblerTemplates.select(stack).isDefined
    }
    else AssemblerTemplates.select(getItem(0)) match {
      case Some(template) =>
        val tplSlot =
          if ((1 until 4) contains slot) template.containerSlots(slot - 1)
          else if ((4 until 13) contains slot) template.upgradeSlots(slot - 4)
          else if ((13 until 21) contains slot) template.componentSlots(slot - 13)
          else AssemblerTemplates.NoSlot
        tplSlot.validate(this, slot, stack)
      case _ => false
    }
}
