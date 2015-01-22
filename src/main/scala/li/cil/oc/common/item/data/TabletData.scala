package li.cil.oc.common.item.data

import li.cil.oc.Settings
import li.cil.oc.common.init.Items
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT

class TabletData extends ItemData {
  def this(stack: ItemStack) {
    this()
    load(stack)
  }

  var items = Array.fill[Option[ItemStack]](32)(None)
  var isRunning = false
  var energy = 0.0
  var maxEnergy = 0.0

  override def load(nbt: NBTTagCompound) {
    nbt.getTagList(Settings.namespace + "items", NBT.TAG_COMPOUND).foreach((slotNbt: NBTTagCompound) => {
      val slot = slotNbt.getByte("slot")
      if (slot >= 0 && slot < items.length) {
        items(slot) = Option(ItemUtils.loadStack(slotNbt.getCompoundTag("item")))
      }
    })
    isRunning = nbt.getBoolean(Settings.namespace + "isRunning")
    energy = nbt.getDouble(Settings.namespace + "energy")
    maxEnergy = nbt.getDouble(Settings.namespace + "maxEnergy")

    // Code for migrating from 1.4.1 -> 1.4.2, add EEPROM.
    // TODO Remove in 1.5
    if (!nbt.hasKey(Settings.namespace + "biosFlag")) {
      val firstEmpty = items.indexWhere(_.isEmpty)
      items(firstEmpty) = Option(Items.createLuaBios())
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

    // TODO Remove in 1.5
    nbt.setBoolean(Settings.namespace + "biosFlag", true)
  }
}
