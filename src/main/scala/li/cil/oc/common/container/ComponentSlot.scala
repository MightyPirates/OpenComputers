package li.cil.oc.common.container

import li.cil.oc.api
import net.minecraft.util.Icon
import net.minecraft.inventory.Slot
import li.cil.oc.common.InventorySlots.Tier

trait ComponentSlot extends Slot {
  def slot: api.driver.Slot

  def tier: Int

  def tierIcon: Icon

  override def func_111238_b() = tier != Tier.None && super.func_111238_b()
}
