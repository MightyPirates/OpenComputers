package li.cil.oc.common.tileentity

import li.cil.oc.api.network.{Analyzable, Visibility}
import li.cil.oc.server.component
import li.cil.oc.{Items, Settings, api}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class Rack extends Environment with Inventory with Analyzable {
  val node = api.Network.newNode(this, Visibility.None).create()

  val servers = Array.fill(getSizeInventory)(None: Option[component.Computer])

  // For client side, where we don't create the component.
  private val _isRunning = Array.fill(getSizeInventory)(None: Option[Boolean])

  private var hasChanged = false

  def markAsChanged() = hasChanged = true

  def getSizeInventory = 4

  def getInvName = Settings.namespace + "container.Rack"

  def getInventoryStackLimit = 1

  def isItemValidForSlot(i: Int, stack: ItemStack) = Items.server.createItemStack().isItemEqual(stack)

  override def updateEntity() {
    super.updateEntity()
    for (server <- servers) server match {
      case Some(computer) => computer.update()
      case _ =>
    }
  }

  override protected def onItemAdded(slot: Int, stack: ItemStack) {
    super.onItemAdded(slot, stack)
    if (isServer) {
      val computer = new component.Computer(new component.Server(this, slot))
      servers(slot) = Some(computer)
      this.node.connect(computer.node)
    }
    else {
      _isRunning(slot) = Some(false)
    }
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    if (isServer) {
      servers(slot) match {
        case Some(computer) => computer.node.remove()
        case _ =>
      }
      servers(slot) = None
    }
    else {
      _isRunning(slot) = None
    }
  }

  def isRunning(number: Int) =
    if (isServer) servers(number) match {
      case Some(server) => server.isRunning
      case _ => false
    }
    else _isRunning(number) match {
      case Some(state) => state
      case _ => false
    }

  def isServerInstalled(number: Int) = if (isServer) servers(number).isDefined else _isRunning(number).isDefined

  def onAnalyze(stats: NBTTagCompound, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = null
}
