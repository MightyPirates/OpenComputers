package li.cil.oc.common.tileentity

import li.cil.oc.server.component
import net.minecraft.item.ItemStack

class Robot extends Computer with ComponentInventory with Rotatable with Redstone {

  val instance = if (true) null else new component.Computer(this)

  def getInvName = ""

  def getSizeInventory = 0

  def isItemValidForSlot(i: Int, itemstack: ItemStack) = false
}
