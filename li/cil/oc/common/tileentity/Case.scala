package li.cil.oc.common.tileentity

import li.cil.oc.Config
import li.cil.oc.api.driver.Slot
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.server.component
import li.cil.oc.server.driver
import li.cil.oc.server.driver.Registry
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection

class Case(isClient: Boolean) extends Computer with ComponentInventory with Rotatable with Redstone {
  def this() = this(false)

  // ----------------------------------------------------------------------- //

  val instance = if (isClient) null else new component.Computer(this)

  private var isRunning = false

  // ----------------------------------------------------------------------- //

  def isOn = isRunning

  def isOn_=(value: Boolean) = {
    isRunning = value
    world.markBlockForRenderUpdate(x, y, z)
    this
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (isServer) {
      if (isRunning != instance.isRunning) {
        isOutputEnabled = hasRedstoneCard && instance.isRunning
        ServerPacketSender.sendComputerState(this, instance.isRunning)
      }
      isRunning = instance.isRunning
      updateRedstoneInput()
    }

    for (component <- components) component match {
      case Some(environment) => environment.update()
      case _ => // Empty.
    }
  }

  override def validate() = {
    super.validate()
    if (isClient) {
      ClientPacketSender.sendRotatableStateRequest(this)
      ClientPacketSender.sendComputerStateRequest(this)
      ClientPacketSender.sendRedstoneStateRequest(this)
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    instance.recomputeMemory()
  }

  // ----------------------------------------------------------------------- //

  def getInvName = Config.namespace + "container.Case"

  def getSizeInventory = 8

  def isItemValidForSlot(slot: Int, item: ItemStack) = (slot, Registry.driverFor(item)) match {
    case (_, None) => false // Invalid item.
    case (0, Some(driver)) => driver.slot(item) == Slot.Power
    case (1 | 2 | 3, Some(driver)) => driver.slot(item) == Slot.Card
    case (4 | 5, Some(driver)) => driver.slot(item) == Slot.Memory
    case (6 | 7, Some(driver)) => driver.slot(item) == Slot.HardDiskDrive
    case _ => false // Invalid slot.
  }

  override def onInventoryChanged() {
    super.onInventoryChanged()
    if (isServer) {
      instance.recomputeMemory()
      isOutputEnabled = hasRedstoneCard && instance.isRunning
    }
  }

  // ----------------------------------------------------------------------- //

  def canConnectRedstone(side: ForgeDirection) = isOutputEnabled

  override protected def onRedstoneInputChanged(side: ForgeDirection) {
    super.onRedstoneInputChanged(side)
    if (isServer) {
      instance.signal("redstone_changed", side.ordinal())
    }
  }

  private def hasRedstoneCard = items.exists {
    case Some(item) => driver.item.RedstoneCard.worksWith(item)
    case _ => false
  }
}