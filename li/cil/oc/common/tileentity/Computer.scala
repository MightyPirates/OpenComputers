package li.cil.oc.common.tileentity

import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.Settings
import li.cil.oc.api.network._
import li.cil.oc.server.{PacketSender => ServerPacketSender, driver, component}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.{NBTTagString, NBTTagCompound}
import net.minecraftforge.common.ForgeDirection
import scala.Some
import scala.collection.mutable
import stargatetech2.api.bus.IBusDevice

// See AbstractBusAware as to why we have to define the IBusDevice here.
@Optional.Interface(iface = "stargatetech2.api.bus.IBusDevice", modid = "StargateTech2")
abstract class Computer(isRemote: Boolean) extends Environment with ComponentInventory with Rotatable with BundledRedstoneAware with AbstractBusAware with IBusDevice with Analyzable with Context with component.Machine.Owner {
  protected val _computer = if (isRemote) null else new component.Machine(this)

  def computer = _computer

  def node = if (isClient) null else computer.node

  override lazy val isClient = computer == null

  private var _isRunning = false

  private var hasChanged = false

  private val _users = mutable.Set.empty[String]

  // ----------------------------------------------------------------------- //

  // Note: we implement IContext in the TE to allow external components to cast
  // their owner to it (to allow interacting with their owning computer).

  def address() = computer.address

  def canInteract(player: String) =
    if (isServer) computer.canInteract(player)
    else !Settings.get.canComputersBeOwned ||
      _users.isEmpty || _users.contains(player)

  def isRunning = _isRunning

  @SideOnly(Side.CLIENT)
  def setRunning(value: Boolean) = {
    _isRunning = value
    world.markBlockForRenderUpdate(x, y, z)
    this
  }

  def isPaused = computer.isPaused

  def start() = computer.start()

  def pause(seconds: Double) = computer.pause(seconds)

  def stop() = computer.stop()

  def signal(name: String, args: AnyRef*) = computer.signal(name, args: _*)

  // ----------------------------------------------------------------------- //

  def markAsChanged() = hasChanged = true

  def installedComponents = components collect {
    case Some(component) => component
  }

  def hasAbstractBusCard = items.exists {
    case Some(item) => computer.isRunning && driver.item.AbstractBusCard.worksWith(item)
    case _ => false
  }

  def hasRedstoneCard = items.exists {
    case Some(item) => computer.isRunning && driver.item.RedstoneCard.worksWith(item)
    case _ => false
  }

  def users: Iterable[String] =
    if (isServer) computer.users
    else _users

  @SideOnly(Side.CLIENT)
  def users_=(list: Iterable[String]) {
    _users.clear()
    _users ++= list
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    if (isServer) {
      // If we're not yet in a network we might have just been loaded from disk,
      // meaning there may be other tile entities that also have not re-joined
      // the network. We skip the update this round to allow other tile entities
      // to join the network, too, avoiding issues of missing nodes (e.g. in the
      // GPU which would otherwise loose track of its screen).
      if (addedToNetwork) {
        computer.update()

        if (hasChanged) {
          hasChanged = false
          world.markTileEntityChunkModified(x, y, z, this)
        }

        if (_isRunning != computer.isRunning) {
          _isRunning = computer.isRunning
          isOutputEnabled = hasRedstoneCard
          isAbstractBusAvailable = hasAbstractBusCard
          ServerPacketSender.sendComputerState(this)
        }

        updateRedstoneInput()
        updateComponents()
      }
    }

    super.updateEntity()
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    computer.load(nbt.getCompoundTag(Settings.namespace + "computer"))
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    nbt.setNewCompoundTag(Settings.namespace + "computer", computer.save)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    setRunning(nbt.getBoolean("isRunning"))
    _users.clear()
    _users ++= nbt.getTagList("users").iterator[NBTTagString].map(_.data)
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setBoolean("isRunning", isRunning)
    nbt.setNewTagList("users", computer.users.map(user => new NBTTagString(null, user)))
  }

  // ----------------------------------------------------------------------- //

  override def onInventoryChanged() {
    super.onInventoryChanged()
    if (isServer) {
      computer.recomputeMemory()
      isOutputEnabled = hasRedstoneCard
      isAbstractBusAvailable = hasAbstractBusCard
    }
  }

  override protected def onRotationChanged() {
    super.onRotationChanged()
    checkRedstoneInputChanged()
  }

  override protected def onRedstoneInputChanged(side: ForgeDirection) {
    super.onRedstoneInputChanged(side)
    computer.signal("redstone_changed", computer.address, Int.box(toLocal(side).ordinal()))
  }

  // ----------------------------------------------------------------------- //

  def onAnalyze(stats: NBTTagCompound, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float): Node = {
    if (computer != null) computer.lastError match {
      case Some(value) =>
        stats.setString(Settings.namespace + "gui.Analyzer.LastError", value)
      case _ =>
    }
    val list = users
    if (list.size > 0) {
      stats.setString(Settings.namespace + "gui.Analyzer.Users", list.mkString(", "))
    }
    computer.node
  }
}
