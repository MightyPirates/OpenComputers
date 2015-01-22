package li.cil.oc.common.item.data

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.Tier
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT

class MicrocontrollerData extends ItemData {
  def this(stack: ItemStack) {
    this()
    load(stack)
  }

  var tier = Tier.One

  var components = Array.empty[ItemStack]

  var storedEnergy = 0

  override def load(nbt: NBTTagCompound) {
    tier = nbt.getByte(Settings.namespace + "tier")
    components = nbt.getTagList(Settings.namespace + "components", NBT.TAG_COMPOUND).
      toArray[NBTTagCompound].map(ItemUtils.loadStack)
    storedEnergy = nbt.getInteger(Settings.namespace + "storedEnergy")
  }

  override def save(nbt: NBTTagCompound) {
    nbt.setByte(Settings.namespace + "tier", tier.toByte)
    nbt.setNewTagList(Settings.namespace + "components", components.toIterable)
    nbt.setInteger(Settings.namespace + "storedEnergy", storedEnergy)
  }

  def createItemStack() = {
    val stack = api.Items.get("microcontroller").createItemStack(1)
    save(stack)
    stack
  }

  def copyItemStack() = {
    val stack = createItemStack()
    val newInfo = new MicrocontrollerData(stack)
    newInfo.save(stack)
    stack
  }
}
