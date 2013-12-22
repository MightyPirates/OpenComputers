package li.cil.oc.common.tileentity

import li.cil.oc.api.network.Visibility
import li.cil.oc.server.component.Server
import li.cil.oc.{Items, Settings, api}
import net.minecraft.item.ItemStack

class Rack extends Environment with Inventory {
  val node = api.Network.newNode(this, Visibility.None).create()

  val servers = Array.fill(getSizeInventory)(None: Option[Server])

  def getSizeInventory = 4

  def getInvName = Settings.namespace + "container.Rack"

  def getInventoryStackLimit = 1

  def isItemValidForSlot(i: Int, stack: ItemStack) = Items.server.createItemStack().isItemEqual(stack)
}
