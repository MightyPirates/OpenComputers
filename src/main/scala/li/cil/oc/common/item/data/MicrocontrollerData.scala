package li.cil.oc.common.item.data

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.Tier
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraftforge.common.util.Constants.NBT

class MicrocontrollerData(itemName: String = Constants.BlockName.Microcontroller) extends ItemData(itemName) {
  def this(stack: ItemStack) {
    this()
    loadData(stack)
  }

  var tier = Tier.One

  var components: Array[ItemStack] = Array[ItemStack](ItemStack.EMPTY)

  var storedEnergy = 0

  private final val TierTag = Settings.namespace + "tier"
  private final val ComponentsTag = Settings.namespace + "components"
  private final val StoredEnergyTag = Settings.namespace + "storedEnergy"

  override def loadData(nbt: CompoundNBT) {
    tier = nbt.getByte(TierTag)
    components = nbt.getList(ComponentsTag, NBT.TAG_COMPOUND).
      toTagArray[CompoundNBT].map(ItemStack.of(_)).filter(!_.isEmpty)
    storedEnergy = nbt.getInt(StoredEnergyTag)

    // Reserve slot for EEPROM if necessary, avoids having to resize the
    // components array in the MCU tile entity, which isn't possible currently.
    if (!components.exists(stack => api.Items.get(stack) == api.Items.get(Constants.ItemName.EEPROM))) {
      components :+= ItemStack.EMPTY
    }
  }

  override def saveData(nbt: CompoundNBT) {
    nbt.putByte(TierTag, tier.toByte)
    nbt.setNewTagList(ComponentsTag, components.filter(!_.isEmpty).toIterable)
    nbt.putInt(StoredEnergyTag, storedEnergy)
  }

  def copyItemStack(): ItemStack = {
    val stack = createItemStack()
    val newInfo = new MicrocontrollerData(stack)
    newInfo.saveData(stack)
    stack
  }
}
