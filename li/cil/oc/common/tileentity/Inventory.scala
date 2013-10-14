package li.cil.oc.common.tileentity

import net.minecraft.entity.item.EntityItem
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.world.World

trait Inventory extends IInventory {
  protected val inventory = Array.fill[Option[ItemStack]](getSizeInventory)(None)

  def getStackInSlot(i: Int) = inventory(i).orNull

  def decrStackSize(slot: Int, amount: Int) = inventory(slot) match {
    case Some(stack) if stack.stackSize <= amount =>
      setInventorySlotContents(slot, null)
      stack
    case Some(stack) =>
      val result = stack.splitStack(amount)
      onInventoryChanged()
      result
    case _ => null
  }

  def setInventorySlotContents(slot: Int, item: ItemStack) = {
    if (inventory(slot).isDefined)
      onItemRemoved(slot, inventory(slot).get)

    inventory(slot) = Option(item)
    if (item != null && item.stackSize > getInventoryStackLimit)
      item.stackSize = getInventoryStackLimit

    if (inventory(slot).isDefined)
      onItemAdded(slot, inventory(slot).get)

    onInventoryChanged()
  }

  def getStackInSlotOnClosing(slot: Int) = null

  def isInvNameLocalized = false

  def openChest() {}

  def closeChest() {}

  def dropContent(world: World, x: Int, y: Int, z: Int) {
    val rng = world.rand
    for (slot <- 0 until getSizeInventory) {
      inventory(slot) match {
        case Some(stack) if stack.stackSize > 0 =>
          setInventorySlotContents(slot, null)
          val (tx, ty, tz) = (0.25 + (rng.nextDouble() * 0.5), 0.25 + (rng.nextDouble() * 0.5), 0.25 + (rng.nextDouble() * 0.5))
          val (vx, vy, vz) = ((rng.nextDouble() - 0.3) * 0.5, (rng.nextDouble() - 0.5) * 0.3, (rng.nextDouble() - 0.5) * 0.3)
          val entity = new EntityItem(world, x + tx, y + ty, z + tz, stack.copy())
          entity.setVelocity(vx, vy, vz)
          entity.delayBeforeCanPickup = 20
          world.spawnEntityInWorld(entity)
        case _ => // Nothing.
      }
    }
  }

  protected def onItemAdded(slot: Int, item: ItemStack) {}

  protected def onItemRemoved(slot: Int, item: ItemStack) {}
}
