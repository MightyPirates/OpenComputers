package li.cil.oc.common.inventory

trait InventorySelection {
  def selectedSlot: Int

  def selectedSlot_=(value: Int): Unit
}
