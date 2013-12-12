package li.cil.oc.common.tileentity

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

abstract class Computer(isRemote: Boolean) extends Environment with ComponentInventory with Rotatable with BundledRedstoneAware with Analyzable {
  protected val _computer = if (isRemote) null else new component.Computer(this)

  def computer = _computer

  def node = if (isClient) null else computer.node

  override lazy val isClient = computer == null

  private var _isRunning = false

  private var hasChanged = false

  private val _users = mutable.Set.empty[String]

  // ----------------------------------------------------------------------- //

  def isRunning = _isRunning

  @SideOnly(Side.CLIENT)
  def isRunning_=(value: Boolean) = {
    _isRunning = value
    world.markBlockForRenderUpdate(x, y, z)
    this
  }

  def markAsChanged() = hasChanged = true

  def hasRedstoneCard = items.exists {
    case Some(item) => driver.item.RedstoneCard.worksWith(item)
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

  def canInteract(player: String) =
    if (isServer) computer.canInteract(player)
    else !Settings.get.canComputersBeOwned ||
      _users.isEmpty || _users.contains(player)

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    if (isServer) {
      // If we're not yet in a network we might have just been loaded from disk,
      // meaning there may be other tile entities that also have not re-joined
      // the network. We skip the update this round to allow other tile entities
      // to join the network, too, avoiding issues of missing nodes (e.g. in the
      // GPU which would otherwise loose track of its screen).
      if (node != null && node.network != null) {
        computer.update()

        if (hasChanged) {
          hasChanged = false
          world.markTileEntityChunkModified(x, y, z, this)
        }

        if (_isRunning != computer.isRunning) {
          _isRunning = computer.isRunning
          isOutputEnabled = hasRedstoneCard && computer.isRunning
          ServerPacketSender.sendComputerState(this)
        }

        updateRedstoneInput()

        if (updatingComponents.length > 0) {
          for (component <- updatingComponents) {
            component.update()
          }
        }
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
    isRunning = nbt.getBoolean("isRunning")
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
      isOutputEnabled = hasRedstoneCard && computer.isRunning
    }
  }

  override protected def onRotationChanged() {
    super.onRotationChanged()
    checkRedstoneInputChanged()
  }

  override protected def onRedstoneInputChanged(side: ForgeDirection) {
    super.onRedstoneInputChanged(side)
    computer.signal("redstone_changed", computer.address, Int.box(side.ordinal()))
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
