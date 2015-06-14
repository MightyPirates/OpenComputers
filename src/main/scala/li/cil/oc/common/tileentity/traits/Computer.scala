package li.cil.oc.common.tileentity.traits

import java.lang
import java.util

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api.Machine
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network.Node
import li.cil.oc.client.Sound
import li.cil.oc.common.tileentity.RobotProxy
import li.cil.oc.common.tileentity.traits
import li.cil.oc.integration.opencomputers.DriverRedstoneCard
import li.cil.oc.integration.stargatetech2.DriverAbstractBusCard
import li.cil.oc.integration.util.Waila
import li.cil.oc.server.agent
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagString
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

trait Computer extends Environment with ComponentInventory with Rotatable with BundledRedstoneAware with AbstractBusAware with Analyzable with MachineHost with StateAware {
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

  def setRunning(value: Boolean): Unit = if (value != _isRunning) {
    _isRunning = value
    if (world != null) {
      world.markBlockForUpdate(x, y, z)
      runSound.foreach(sound =>
        if (_isRunning) Sound.startLoop(this, sound, 0.5f, 50 + world.rand.nextInt(50))
        else Sound.stopLoop(this)
      )
    }
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

  override def internalComponents(): lang.Iterable[ItemStack] = (0 until getSizeInventory).collect {
    case slot if getStackInSlot(slot) != null && isComponentSlot(slot, getStackInSlot(slot)) => getStackInSlot(slot)
  }

  override def installedComponents = components collect {
    case Some(component) => component
  }

  override def onMachineConnect(node: Node) = this.onConnect(node)

  override def onMachineDisconnect(node: Node) = this.onDisconnect(node)

  def hasAbstractBusCard = items.exists {
    case Some(item) => machine.isRunning && DriverAbstractBusCard.worksWith(item, getClass)
    case _ => false
  }

  def hasRedstoneCard = items.exists {
    case Some(item) => machine.isRunning && DriverRedstoneCard.worksWith(item, getClass)
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

      if (_isRunning != machine.isRunning) {
        _isRunning = machine.isRunning
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
    if (machine != null && !this.isInstanceOf[RobotProxy] && !moving) {
      machine.stop()
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    // God, this is so ugly... will need to rework the robot architecture.
    // This is required for loading auxiliary data (kernel state), because the
    // coordinates in the actual robot won't be set properly, otherwise.
    this match {
      case proxy: RobotProxy =>
        proxy.robot.xCoord = xCoord
        proxy.robot.yCoord = yCoord
        proxy.robot.zCoord = zCoord
      case _ =>
    }
    machine.load(nbt.getCompoundTag(Settings.namespace + "computer"))

    // Kickstart initialization to avoid values getting overwritten by
    // readFromNBTForClient if that packet is handled after a manual
    // initialization / state change packet.
    setRunning(machine.isRunning)
    _isOutputEnabled = hasRedstoneCard
    _isAbstractBusAvailable = hasAbstractBusCard
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    if (machine != null) {
      if (!Waila.isSavingForTooltip)
        nbt.setNewCompoundTag(Settings.namespace + "computer", machine.save)
      else if (machine.node.address != null)
        nbt.setString(Settings.namespace + "address", machine.node.address)
    }
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    setRunning(nbt.getBoolean("isRunning"))
    _users.clear()
    _users ++= nbt.getTagList("users", NBT.TAG_STRING).map((tag: NBTTagString) => tag.func_150285_a_())
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
      isAbstractBusAvailable = hasAbstractBusCard
    }
  }

  override def isUseableByPlayer(player: EntityPlayer) =
    super.isUseableByPlayer(player) && (player match {
      case fakePlayer: agent.Player => canInteract(fakePlayer.agent.ownerName())
      case _ => canInteract(player.getCommandSenderName)
    })

  override protected def onRotationChanged() {
    super.onRotationChanged()
    checkRedstoneInputChanged()
  }

  override protected def onRedstoneInputChanged(side: ForgeDirection, oldMaxValue: Int, newMaxValue: Int) {
    super.onRedstoneInputChanged(side, oldMaxValue, newMaxValue)
    machine.node.sendToNeighbors("redstone.changed", toLocal(side), int2Integer(oldMaxValue), int2Integer(newMaxValue))
  }

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = Array(machine.node)
}
