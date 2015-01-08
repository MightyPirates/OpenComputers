package li.cil.oc.common.tileentity.traits

import java.util

import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.api.Driver
import li.cil.oc.api.Machine
import li.cil.oc.api.driver.item.Processor
import li.cil.oc.api.machine.Architecture
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network.Node
import li.cil.oc.client.Sound
import li.cil.oc.common.Slot
import li.cil.oc.common.tileentity.RobotProxy
import li.cil.oc.common.tileentity.traits
import li.cil.oc.integration.opencomputers.DriverRedstoneCard
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagString
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

import scala.collection.mutable

trait Computer extends Environment with ComponentInventory with Rotatable with BundledRedstoneAware with Analyzable with MachineHost with StateAware {
  private lazy val _machine = if (isServer) Machine.create(this) else null

  def machine = _machine

  override def node = if (isServer) machine.node else null

  private var _isRunning = false

  private val _users = mutable.Set.empty[String]

  protected def runSound = Option("computer_running")

  // ----------------------------------------------------------------------- //

  def canInteract(player: String) =
    if (isServer) machine.canInteract(player)
    else !Settings.get.canComputersBeOwned || _users.isEmpty || _users.contains(player)

  def isRunning = _isRunning

  @SideOnly(Side.CLIENT)
  def setRunning(value: Boolean): Unit = if (value != _isRunning) {
    _isRunning = value
    world.markBlockForUpdate(getPos)
    runSound.foreach(sound =>
      if (_isRunning) Sound.startLoop(this, sound, 0.5f, 50 + world.rand.nextInt(50))
      else Sound.stopLoop(this)
    )
  }

  @SideOnly(Side.CLIENT)
  def setUsers(list: Iterable[String]) {
    _users.clear()
    _users ++= list
  }

  override def currentState = {
    if (isRunning) util.EnumSet.of(traits.State.IsWorking)
    else util.EnumSet.noneOf(classOf[traits.State])
  }

  // ----------------------------------------------------------------------- //

  override def cpuArchitecture: Class[_ <: Architecture] = {
    for (i <- 0 until getSizeInventory if isComponentSlot(i)) Option(getStackInSlot(i)) match {
      case Some(s) => Option(Driver.driverFor(s, getClass)) match {
        case Some(driver: Processor) if driver.slot(s) == Slot.CPU => return driver.architecture(s)
        case _ =>
      }
      case _ =>
    }
    null
  }


  override def onMachineConnect(node: Node) = this.onConnect(node)

  override def onMachineDisconnect(node: Node) = this.onDisconnect(node)

  def hasRedstoneCard = items.exists {
    case Some(item) => machine.isRunning && DriverRedstoneCard.worksWith(item, getClass)
    case _ => false
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    if (isServer && isConnected) {
      // If we're not yet in a network we might have just been loaded from disk,
      // meaning there may be other tile entities that also have not re-joined
      // the network. We skip the update this round to allow other tile entities
      // to join the network, too, avoiding issues of missing nodes (e.g. in the
      // GPU which would otherwise loose track of its screen).
      machine.update()

      if (_isRunning != machine.isRunning) {
        _isRunning = machine.isRunning
        markDirty()
        ServerPacketSender.sendComputerState(this)
      }

      updateComponents()
    }

    super.updateEntity()
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    // God, this is so ugly... will need to rework the robot architecture.
    // This is required for loading auxiliary data (kernel state), because the
    // coordinates in the actual robot won't be set properly, otherwise.
    this match {
      case proxy: RobotProxy => proxy.robot.setPos(getPos)
      case _ =>
    }
    machine.load(nbt.getCompoundTag(Settings.namespace + "computer"))

    // Kickstart initialization to avoid values getting overwritten by
    // readFromNBTForClient if that packet is handled after a manual
    // initialization / state change packet.
    _isRunning = machine.isRunning
    _isOutputEnabled = hasRedstoneCard
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    if (machine != null) {
      nbt.setNewCompoundTag(Settings.namespace + "computer", machine.save)
    }
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    _isRunning = nbt.getBoolean("isRunning")
    _users.clear()
    _users ++= nbt.getTagList("users", NBT.TAG_STRING).map((tag: NBTTagString) => tag.getString)
    if (_isRunning) runSound.foreach(sound => Sound.startLoop(this, sound, 0.5f, 1000 + world.rand.nextInt(2000)))
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setBoolean("isRunning", isRunning)
    nbt.setNewTagList("users", machine.users.map(user => new NBTTagString(user)))
  }

  // ----------------------------------------------------------------------- //

  override def markDirty() {
    super.markDirty()
    if (isServer) {
      machine.onHostChanged()
      isOutputEnabled = hasRedstoneCard
    }
  }

  override def isUseableByPlayer(player: EntityPlayer) =
    super.isUseableByPlayer(player) && canInteract(player.getName)

  override protected def onRotationChanged() {
    super.onRotationChanged()
    checkRedstoneInputChanged()
  }

  override protected def onRedstoneInputChanged(side: EnumFacing) {
    super.onRedstoneInputChanged(side)
    machine.signal("redstone_changed", machine.node.address, Int.box(toLocal(side).ordinal()))
  }

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = {
    machine.lastError match {
      case value if value != null =>
        player.addChatMessage(Localization.Analyzer.LastError(value))
      case _ =>
    }
    player.addChatMessage(Localization.Analyzer.Components(machine.componentCount, maxComponents))
    val list = machine.users
    if (list.size > 0) {
      player.addChatMessage(Localization.Analyzer.Users(list))
    }
    Array(machine.node)
  }
}
