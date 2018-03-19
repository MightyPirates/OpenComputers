package li.cil.oc.common.tileentity.traits

import java.lang
import java.util

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.machine.Machine
import li.cil.oc.api.network.Node
import li.cil.oc.client.Sound
import li.cil.oc.common.tileentity.RobotProxy
import li.cil.oc.integration.opencomputers.DriverRedstoneCard
import li.cil.oc.server.agent
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagString
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

trait Computer extends Environment with ComponentInventory with Rotatable with BundledRedstoneAware with api.network.Analyzable with api.machine.MachineHost with StateAware with Tickable {
  private lazy val _machine = if (isServer) api.Machine.create(this) else null

  def machine: Machine = _machine

  override def node: Node = if (isServer) machine.node else null

  private var _isRunning = false

  // For client side rendering of error LED indicator.
  var hasErrored = false

  private val _users = mutable.Set.empty[String]

  protected def runSound = Option("computer_running")

  // ----------------------------------------------------------------------- //

  def canInteract(player: String): Boolean =
    if (isServer) machine.canInteract(player)
    else !Settings.get.canComputersBeOwned || _users.isEmpty || _users.contains(player)

  def isRunning: Boolean = _isRunning

  def setRunning(value: Boolean): Unit = if (value != _isRunning) {
    _isRunning = value
    if (value) {
      hasErrored = false
    }
    if (getWorld != null) {
      getWorld.notifyBlockUpdate(getPos, getWorld.getBlockState(getPos), getWorld.getBlockState(getPos), 3)
      if (getWorld.isRemote) {
        runSound.foreach(sound =>
          if (_isRunning) Sound.startLoop(this, sound, 0.5f, 50 + getWorld.rand.nextInt(50))
          else Sound.stopLoop(this)
        )
      }
    }
  }

  @SideOnly(Side.CLIENT)
  def setUsers(list: Iterable[String]) {
    _users.clear()
    _users ++= list
  }

  override def getCurrentState: util.EnumSet[api.util.StateAware.State] = {
    if (isRunning) util.EnumSet.of(api.util.StateAware.State.IsWorking)
    else util.EnumSet.noneOf(classOf[api.util.StateAware.State])
  }

  // ----------------------------------------------------------------------- //

  override def internalComponents(): lang.Iterable[ItemStack] = (0 until getSizeInventory).collect {
    case slot if !getStackInSlot(slot).isEmpty && isComponentSlot(slot, getStackInSlot(slot)) => getStackInSlot(slot)
  }


  override def onMachineConnect(node: api.network.Node): Unit = this.onConnect(node)

  override def onMachineDisconnect(node: api.network.Node): Unit = this.onDisconnect(node)

  def hasRedstoneCard: Boolean = items.exists {
    case item if !item.isEmpty => machine.isRunning && DriverRedstoneCard.worksWith(item, getClass)
    case _ => false
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity(): Unit = {
    // If we're not yet in a network we might have just been loaded from disk,
    // meaning there may be other tile entities that also have not re-joined
    // the network. We skip the update this round to allow other tile entities
    // to join the network, too, avoiding issues of missing nodes (e.g. in the
    // GPU which would otherwise loose track of its screen).
    if (isServer && isConnected) {
      updateComputer()

      val running = machine.isRunning
      val errored = machine.lastError != null
      if (_isRunning != running || hasErrored != errored) {
        _isRunning = running
        hasErrored = errored
        onRunningChanged()
      }

      updateComponents()
    }

    super.updateEntity()
  }

  protected def updateComputer(): Unit = {
    machine.update()
  }

  protected def onRunningChanged(): Unit = {
    markDirty()
    ServerPacketSender.sendComputerState(this)
  }

  override def dispose(): Unit = {
    super.dispose()
    if (machine != null && !this.isInstanceOf[RobotProxy]) {
      machine.stop()
    }
  }

  // ----------------------------------------------------------------------- //

  private final val ComputerTag = Settings.namespace + "computer"
  private final val HasErroredTag = Settings.namespace + "hasErrored"
  private final val IsRunningTag = Settings.namespace + "isRunning"
  private final val UsersTag = Settings.namespace + "users"

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    // God, this is so ugly... will need to rework the robot architecture.
    // This is required for loading auxiliary data (kernel state), because the
    // coordinates in the actual robot won't be set properly, otherwise.
    this match {
      case proxy: RobotProxy => proxy.robot.setPos(getPos)
      case _ =>
    }
    machine.load(nbt.getCompoundTag(ComputerTag))

    // Kickstart initialization to avoid values getting overwritten by
    // readFromNBTForClient if that packet is handled after a manual
    // initialization / state change packet.
    setRunning(machine.isRunning)
    _isOutputEnabled = hasRedstoneCard
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    if (machine != null) {
      nbt.setNewCompoundTag(ComputerTag, machine.save)
    }
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    hasErrored = nbt.getBoolean(HasErroredTag)
    setRunning(nbt.getBoolean(IsRunningTag))
    _users.clear()
    _users ++= nbt.getTagList(UsersTag, NBT.TAG_STRING).map((tag: NBTTagString) => tag.getString)
    if (_isRunning) runSound.foreach(sound => Sound.startLoop(this, sound, 0.5f, 1000 + getWorld.rand.nextInt(2000)))
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setBoolean(HasErroredTag, machine != null && machine.lastError != null)
    nbt.setBoolean(IsRunningTag, isRunning)
    nbt.setNewTagList(UsersTag, machine.users.map(user => new NBTTagString(user)))
  }

  // ----------------------------------------------------------------------- //

  override def markDirty() {
    super.markDirty()
    if (isServer) {
      machine.onHostChanged()
      isOutputEnabled = hasRedstoneCard
    }
  }

  override def isUsableByPlayer(player: EntityPlayer): Boolean =
    super.isUsableByPlayer(player) && (player match {
      case fakePlayer: agent.Player => canInteract(fakePlayer.agent.ownerName())
      case _ => canInteract(player.getName)
    })

  override protected def onRotationChanged() {
    super.onRotationChanged()
    checkRedstoneInputChanged()
  }

  override protected def onRedstoneInputChanged(args: RedstoneChangedEventArgs) {
    super.onRedstoneInputChanged(args)
    machine.node.sendToNeighbors("redstone.changed", args)
  }

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = Array(machine.node)
}
