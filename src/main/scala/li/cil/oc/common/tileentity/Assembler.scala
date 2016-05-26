package li.cil.oc.common.tileentity

import java.util

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.common.template.AssemblerTemplates
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class Assembler extends traits.Environment with traits.PowerAcceptor with traits.Inventory with SidedEnvironment with traits.StateAware with traits.Tickable {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("assembler").
    withConnector(Settings.get.bufferConverter).
    create()

  var output: Option[ItemStack] = None

  var totalRequiredEnergy = 0.0

  var requiredEnergy = 0.0

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def canConnect(side: EnumFacing) = side != EnumFacing.UP

  override def sidedNode(side: EnumFacing) = if (side != EnumFacing.UP) node else null

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: EnumFacing) = canConnect(side)

  override protected def connector(side: EnumFacing) = Option(if (side != EnumFacing.UP) node else null)

  override def energyThroughput = Settings.get.assemblerRate

  override def getCurrentState = {
    if (isAssembling) util.EnumSet.of(api.util.StateAware.State.IsWorking)
    else if (canAssemble) util.EnumSet.of(api.util.StateAware.State.CanWork)
    else util.EnumSet.noneOf(classOf[api.util.StateAware.State])
  }

  // ----------------------------------------------------------------------- //

  def canAssemble = AssemblerTemplates.select(getStackInSlot(0)) match {
    case Some(template) => !isAssembling && output.isEmpty && template.validate(this)._1
    case _ => false
  }

  def isAssembling = requiredEnergy > 0

  def progress = (1 - requiredEnergy / totalRequiredEnergy) * 100

  def timeRemaining = (requiredEnergy / Settings.get.assemblerTickAmount / 20).toInt

  // ----------------------------------------------------------------------- //

  def start(finishImmediately: Boolean = false): Boolean = this.synchronized {
    AssemblerTemplates.select(getStackInSlot(0)) match {
      case Some(template) if !isAssembling && output.isEmpty && template.validate(this)._1 =>
        for (slot <- 0 until getSizeInventory) {
          val stack = getStackInSlot(slot)
          if (stack != null && !isItemValidForSlot(slot, stack)) return false
        }
        val (stack, energy) = template.assemble(this)
        output = Some(stack)
        if (finishImmediately) {
          totalRequiredEnergy = 0
        }
        else {
          totalRequiredEnergy = math.max(1, energy)
        }
        requiredEnergy = totalRequiredEnergy
        ServerPacketSender.sendRobotAssembling(this, assembling = true)

        for (slot <- 0 until getSizeInventory) updateItems(slot, null)
        markDirty()

        true
      case _ => false
    }
  }

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function(): string, number or boolean -- The current state of the assembler, `busy' or `idle', followed by the progress or template validity, respectively.""")
  def status(context: Context, args: Arguments): Array[Object] = {
    if (isAssembling) result("busy", progress)
    else AssemblerTemplates.select(getStackInSlot(0)) match {
      case Some(template) if template.validate(this)._1 => result("idle", true)
      case _ => result("idle", false)
    }
  }

  @Callback(doc = """function():boolean -- Start assembling, if possible. Returns whether assembly was started or not.""")
  def start(context: Context, args: Arguments): Array[Object] = result(start())

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (output.isDefined && world.getTotalWorldTime % Settings.get.tickFrequency == 0) {
      val want = math.max(1, math.min(requiredEnergy, Settings.get.assemblerTickAmount * Settings.get.tickFrequency))
      val have = want + (if (Settings.get.ignorePower) 0 else node.changeBuffer(-want))
      requiredEnergy -= have
      if (requiredEnergy <= 0) {
        setInventorySlotContents(0, output.get)
        output = None
        requiredEnergy = 0
      }
      ServerPacketSender.sendRobotAssembling(this, have > 0.5 && output.isDefined)
    }
  }

  // ----------------------------------------------------------------------- //

  private final val OutputTag = Settings.namespace + "output"
  private final val OutputTagCompat = Settings.namespace + "robot"
  private final val TotalTag = Settings.namespace + "total"
  private final val RemainingTag = Settings.namespace + "remaining"

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    if (nbt.hasKey(OutputTag)) {
      output = Option(ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(OutputTag)))
    }
    else if (nbt.hasKey(OutputTagCompat)) {
      output = Option(ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(OutputTagCompat)))
    }
    totalRequiredEnergy = nbt.getDouble(TotalTag)
    requiredEnergy = nbt.getDouble(RemainingTag)
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    output.foreach(stack => nbt.setNewCompoundTag(OutputTag, stack.writeToNBT))
    nbt.setDouble(TotalTag, totalRequiredEnergy)
    nbt.setDouble(RemainingTag, requiredEnergy)
  }

  @SideOnly(Side.CLIENT) override
  def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    requiredEnergy = nbt.getDouble(RemainingTag)
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setDouble(RemainingTag, requiredEnergy)
  }

  // ----------------------------------------------------------------------- //

  override def getSizeInventory = 22

  override def getInventoryStackLimit = 1

  override def isItemValidForSlot(slot: Int, stack: ItemStack) =
    if (slot == 0) {
      !isAssembling && AssemblerTemplates.select(stack).isDefined
    }
    else AssemblerTemplates.select(getStackInSlot(0)) match {
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
