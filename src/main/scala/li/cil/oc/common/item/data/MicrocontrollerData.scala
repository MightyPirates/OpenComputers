package li.cil.oc.common.item.data

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.Tier
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT

class MicrocontrollerData(itemName: String = Constants.BlockName.Microcontroller) extends ItemData(itemName) {
  def this(stack: ItemStack) {
    this()
    load(stack)
  }

  var tier = Tier.One

  var components = Array[ItemStack](null)

  var storedEnergy = 0

  override def load(nbt: NBTTagCompound) {
    tier = nbt.getByte(Settings.namespace + "tier")
    components = nbt.getTagList(Settings.namespace + "components", NBT.TAG_COMPOUND).
      toArray[NBTTagCompound].map(ItemStack.loadItemStackFromNBT).filter(_ != null)
    storedEnergy = nbt.getInteger(Settings.namespace + "storedEnergy")

    // Reserve slot for EEPROM if necessary, avoids having to resize the
    // components array in the MCU tile entity, which isn't possible currently.
    if (!components.exists(stack => api.Items.get(stack) == api.Items.get(Constants.ItemName.EEPROM))) {
      components :+= null
    }
  }

  override def save(nbt: NBTTagCompound) {
    nbt.setByte(Settings.namespace + "tier", tier.toByte)
    nbt.setNewTagList(Settings.namespace + "components", components.filter(_ != null).toIterable)
    nbt.setInteger(Settings.namespace + "storedEnergy", storedEnergy)
  }

  def copyItemStack() = {
    val stack = createItemStack()
    val newInfo = new MicrocontrollerData(stack)
    newInfo.save(stack)
    stack
  }
}
