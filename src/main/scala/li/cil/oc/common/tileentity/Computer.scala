package li.cil.oc.common.tileentity

import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.Settings
import li.cil.oc.api.network._
import li.cil.oc.server.component.machine.Machine
import li.cil.oc.server.{PacketSender => ServerPacketSender, driver}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.{NBTTagString, NBTTagCompound}
import net.minecraft.util.ChatComponentTranslation
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.common.util.ForgeDirection
import scala.Some
import scala.collection.mutable
import stargatetech2.api.bus.IBusDevice

// See AbstractBusAware as to why we have to define the IBusDevice here.
@Optional.Interface(iface = "stargatetech2.api.bus.IBusDevice", modid = "StargateTech2")
abstract class Computer(isRemote: Boolean) extends Environment with ComponentInventory with Rotatable with BundledRedstoneAware with AbstractBusAware with IBusDevice with Analyzable with Machine.Owner {
  protected val _computer = if (isRemote) null else new Machine(this)

  def computer = _computer

  override def node = if (isClient) null else computer.node

  override lazy val isClient = computer == null

  private var _isRunning = false

  private var hasChanged = false

  private val _users = mutable.Set.empty[String]

  // ----------------------------------------------------------------------- //

  // Note: we implement IContext in the TE to allow external components to cast
  // their owner to it (to allow interacting with their owning computer).

  override def address = computer.address

  override def canInteract(player: String) =
    if (isServer) computer.canInteract(player)
    else !Settings.get.canComputersBeOwned ||
      _users.isEmpty || _users.contains(player)

  override def isRunning = _isRunning

  @SideOnly(Side.CLIENT)
  def setRunning(value: Boolean) = {
    _isRunning = value
    world.markBlockForUpdate(x, y, z)
    this
  }

  override def isPaused = computer.isPaused

  override def start() = computer.start()

  override def pause(seconds: Double) = computer.pause(seconds)

  override def stop() = computer.stop()

  override def signal(name: String, args: AnyRef*) = computer.signal(name, args: _*)

  // ----------------------------------------------------------------------- //

  override def markAsChanged() = hasChanged = true

  override def installedComponents = components collect {
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
    if (!new Exception().getStackTrace.exists(_.getClassName.startsWith("mcp.mobius.waila"))) {
      nbt.setNewCompoundTag(Settings.namespace + "computer", computer.save)
    }
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    setRunning(nbt.getBoolean("isRunning"))
    _users.clear()
    _users ++= nbt.getTagList("users", NBT.TAG_STRING).map((list, index) => list.getStringTagAt(index))
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setBoolean("isRunning", isRunning)
    nbt.setNewTagList("users", computer.users.map(user => new NBTTagString(user)))
  }

  // ----------------------------------------------------------------------- //

  override def markDirty() {
    super.markDirty()
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
    computer.signal("redstone_changed", computer.node.address, Int.box(toLocal(side).ordinal()))
  }

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    computer.lastError match {
      case Some(value) =>
        player.addChatMessage(new ChatComponentTranslation(
          Settings.namespace + "gui.Analyzer.LastError", new ChatComponentTranslation(value)))
      case _ =>
    }
    player.addChatMessage(new ChatComponentTranslation(
      Settings.namespace + "gui.Analyzer.Components", computer.componentCount + "/" + maxComponents))
    val list = users
    if (list.size > 0) {
      player.addChatMessage(new ChatComponentTranslation(
        Settings.namespace + "gui.Analyzer.Users", list.mkString(", ")))
    }
    Array(computer.node)
  }
}
