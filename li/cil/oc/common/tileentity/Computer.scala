package li.cil.oc.common.tileentity

import cpw.mods.fml.common.Loader
import li.cil.oc.Config
import li.cil.oc.api.Network
import li.cil.oc.api.driver.Slot
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.server.component
import li.cil.oc.server.component.Computer.{Environment => ComputerEnvironment}
import li.cil.oc.server.component.Redstone
import li.cil.oc.server.driver
import li.cil.oc.server.driver.Registry
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import mods.immibis.redlogic.api.wiring.IBundledEmitter
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection

class Computer(isClient: Boolean) extends Rotatable with ComputerEnvironment with ComponentInventory with Redstone {
  def this() = this(false)

  // ----------------------------------------------------------------------- //

  private var powerConsumed = 0.0

  private var hasChanged = false

  private var isRunning = false

  // ----------------------------------------------------------------------- //

  val instance = if (isClient) null else new component.Computer(this)

  def world = worldObj

  def markAsChanged(power: Double) = this.synchronized {
    powerConsumed = (powerConsumed + power) max 0
    hasChanged = true
  }

  // ----------------------------------------------------------------------- //

  def turnOn() = instance.start()

  def turnOff() = instance.stop()

  def isOn = isRunning

  def isOn_=(value: Boolean) = {
    isRunning = value
    worldObj.markBlockForRenderUpdate(xCoord, yCoord, zCoord)
    this
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    super.load(nbt)
    instance.recomputeMemory()
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    super.save(nbt)
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (node != null && node.network == null) {
      Network.joinOrCreateNetwork(worldObj, xCoord, yCoord, zCoord)
      this.synchronized(powerConsumed = 0.0)
    }
    else if (!worldObj.isRemote) {
      // If we just joined a network we were just loaded from disk. We skip the
      // update this round to allow other tile entities to join the network,
      // too, avoiding issues of missing nodes (e.g. in the GPU which would
      // otherwise loose track of its screen).
      instance.update()
      val (powerRequired, needsSaving) = this.synchronized {
        val a = powerConsumed + Config.computerBaseCost
        val b = hasChanged
        powerConsumed = 0
        hasChanged = false
        (a, b)
      }
      if (isRunning && !node.changeBuffer(-powerRequired)) {
        // TODO try to print to screen? sound effect? particle effect?
        println("not enough power, shutting down... " + powerRequired)
        turnOff()
      }
      if (needsSaving)
        worldObj.markTileEntityChunkModified(xCoord, yCoord, zCoord, this)
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

  override def validate() {
    super.validate()
    if (worldObj.isRemote) {
      ClientPacketSender.sendComputerStateRequest(this)
      ClientPacketSender.sendRedstoneStateRequest(this)
    }
  }

  // ----------------------------------------------------------------------- //

  def getInvName = "oc.container.Computer"

  def getSizeInventory = 8

  def isItemValidForSlot(slot: Int, item: ItemStack) = (slot, Registry.driverFor(item)) match {
    case (_, None) => false // Invalid item.
    case (0, Some(driver)) => driver.slot(item) == Slot.Power
    case (1 | 2 | 3, Some(driver)) => driver.slot(item) == Slot.Card
    case (4 | 5, Some(driver)) => driver.slot(item) == Slot.Memory
    case (6 | 7, Some(driver)) => driver.slot(item) == Slot.HardDiskDrive
    case _ => false // Invalid slot.
  }

  override def isUseableByPlayer(player: EntityPlayer) =
    worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this &&
      player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) < 64

  override def onInventoryChanged() {
    super.onInventoryChanged()
    if (!worldObj.isRemote) {
      instance.recomputeMemory()
      isOutputEnabled = hasRedstoneCard && instance.isRunning
    }
  }

  // ----------------------------------------------------------------------- //

  def canConnectRedstone(side: ForgeDirection) = isOutputEnabled

  override def computeInput(side: ForgeDirection) = {
    val global = toGlobal(side)
    worldObj.getIndirectPowerLevelTo(
      xCoord + global.offsetX,
      yCoord + global.offsetY,
      zCoord + global.offsetZ,
      global.ordinal())
  }

  protected def computeBundledInput(side: ForgeDirection) = {
    val global = toGlobal(side)
    if (Loader.isModLoaded("RedLogic")) {
      worldObj.getBlockTileEntity(
        xCoord + global.offsetX,
        yCoord + global.offsetY,
        zCoord + global.offsetZ) match {
        case emitter: IBundledEmitter =>
          var strength: Array[Byte] = null
          for (i <- -1 to 5 if strength == null) {
            strength = emitter.getBundledCableStrength(i, global.getOpposite.ordinal())
          }
          strength
        case _ => null
      }
    } else null
  }

  override protected def onRedstoneInputChanged(side: ForgeDirection) {
    super.onRedstoneInputChanged(side)
    instance.signal("redstone_changed", side.ordinal())
  }

  override protected def onRedstoneOutputChanged(side: ForgeDirection) {
    super.onRedstoneOutputChanged(side)
    if (side == ForgeDirection.UNKNOWN) {
      worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord,
        getBlockType.blockID)
    }
    else {
      val global = toGlobal(side)
      worldObj.notifyBlockOfNeighborChange(
        xCoord + global.offsetX,
        yCoord + global.offsetY,
        zCoord + global.offsetZ,
        getBlockType.blockID)
    }
    if (!worldObj.isRemote) ServerPacketSender.sendRedstoneState(this)
    else worldObj.markBlockForRenderUpdate(xCoord, yCoord, zCoord)
  }

  private def hasRedstoneCard = inventory.exists {
    case Some(item) => driver.RedstoneCard.worksWith(item)
    case _ => false
  }
}