package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.Settings
import li.cil.oc.api.network._
import li.cil.oc.server.{PacketSender => ServerPacketSender, driver, component}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection
import scala.Some

abstract class Computer(isRemote: Boolean) extends Environment with ComponentInventory with Rotatable with BundledRedstone with Analyzable {
  protected val _computer = if (isRemote) null else new component.Computer(this)

  def computer = _computer

  def node = if (isClient) null else computer.node

  override def isClient = computer == null

  private var _isRunning = false

  private var hasChanged = false

  // ----------------------------------------------------------------------- //

  def isRunning = _isRunning

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

        for (component <- components) component match {
          case Some(environment) => environment.update()
          case _ => // Empty.
        }
      }
    }

    super.updateEntity()
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (isServer) {
      computer.load(nbt.getCompoundTag(Settings.namespace + "computer"))
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    if (isServer) {
      nbt.setNewCompoundTag(Settings.namespace + "computer", computer.save)
    }
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    isRunning = nbt.getBoolean("isRunning")
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setBoolean("isRunning", isRunning)
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
    computer.signal("redstone_changed", Int.box(side.ordinal()))
  }

  // ----------------------------------------------------------------------- //

  def onAnalyze(stats: NBTTagCompound, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float): Node = {
    if (computer != null) computer.lastError match {
      case Some(value) => stats.setString(Settings.namespace + "gui.Analyzer.LastError", value)
      case _ =>
    }
    computer.node
  }
}
