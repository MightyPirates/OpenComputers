package li.cil.oc.common.item.data

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.common.Tier
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT

class TabletData extends ItemData(Constants.ItemName.Tablet) {
  def this(stack: ItemStack) {
    this()
    load(stack)
  }

  var items = Array.fill[Option[ItemStack]](32)(None)
  var isRunning = false
  var energy = 0.0
  var maxEnergy = 0.0
  var tier = Tier.One
  var container: Option[ItemStack] = None

  override def load(nbt: NBTTagCompound) {
    nbt.getTagList(Settings.namespace + "items", NBT.TAG_COMPOUND).foreach((slotNbt: NBTTagCompound) => {
      val slot = slotNbt.getByte("slot")
      if (slot >= 0 && slot < items.length) {
        items(slot) = Option(ItemStack.loadItemStackFromNBT(slotNbt.getCompoundTag("item")))
      }
    })
    isRunning = nbt.getBoolean(Settings.namespace + "isRunning")
    energy = nbt.getDouble(Settings.namespace + "energy")
    maxEnergy = nbt.getDouble(Settings.namespace + "maxEnergy")
    tier = nbt.getInteger(Settings.namespace + "tier")
    if (nbt.hasKey(Settings.namespace + "container")) {
      container = Option(ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(Settings.namespace + "container")))
    }
  }

  override def save(nbt: NBTTagCompound) {
    nbt.setNewTagList(Settings.namespace + "items",
      items.zipWithIndex collect {
        case (Some(stack), slot) => (stack, slot)
      } map {
        case (stack, slot) =>
          val slotNbt = new NBTTagCompound()
          slotNbt.setByte("slot", slot.toByte)
          slotNbt.setNewCompoundTag("item", stack.writeToNBT)
      })
    nbt.setBoolean(Settings.namespace + "isRunning", isRunning)
    nbt.setDouble(Settings.namespace + "energy", energy)
    nbt.setDouble(Settings.namespace + "maxEnergy", maxEnergy)
    nbt.setInteger(Settings.namespace + "tier", tier)
    container.foreach(stack => nbt.setNewCompoundTag(Settings.namespace + "container", stack.writeToNBT))
  }
}
