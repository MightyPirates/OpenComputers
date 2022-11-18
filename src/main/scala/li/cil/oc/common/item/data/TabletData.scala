package li.cil.oc.common.item.data

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.common.Tier
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraftforge.common.util.Constants.NBT

class TabletData extends ItemData(Constants.ItemName.Tablet) {
  def this(stack: ItemStack) {
    this()
    loadData(stack)
  }

  var items = Array.fill[ItemStack](32)(ItemStack.EMPTY)
  var isRunning = false
  var energy = 0.0
  var maxEnergy = 0.0
  var tier = Tier.One
  var container = ItemStack.EMPTY

  private final val ItemsTag = Settings.namespace + "items"
  private final val SlotTag = "slot"
  private final val ItemTag = "item"
  private final val IsRunningTag = Settings.namespace + "isRunning"
  private final val EnergyTag = Settings.namespace + "energy"
  private final val MaxEnergyTag = Settings.namespace + "maxEnergy"
  private final val TierTag = Settings.namespace + "tier"
  private final val ContainerTag = Settings.namespace + "container"

  override def loadData(nbt: CompoundNBT) {
    nbt.getList(ItemsTag, NBT.TAG_COMPOUND).foreach((slotNbt: CompoundNBT) => {
      val slot = slotNbt.getByte(SlotTag)
      if (slot >= 0 && slot < items.length) {
        items(slot) = ItemStack.of(slotNbt.getCompound(ItemTag))
      }
    })
    isRunning = nbt.getBoolean(IsRunningTag)
    energy = nbt.getDouble(EnergyTag)
    maxEnergy = nbt.getDouble(MaxEnergyTag)
    tier = nbt.getInt(TierTag)
    if (nbt.contains(ContainerTag)) {
      container = ItemStack.of(nbt.getCompound(ContainerTag))
    }
  }

  override def saveData(nbt: CompoundNBT) {
    nbt.setNewTagList(ItemsTag,
      items.zipWithIndex collect {
        case (stack, slot) if !stack.isEmpty => (stack, slot)
      } map {
        case (stack, slot) =>
          val slotNbt = new CompoundNBT()
          slotNbt.putByte(SlotTag, slot.toByte)
          slotNbt.setNewCompoundTag(ItemTag, stack.save)
      })
    nbt.putBoolean(IsRunningTag, isRunning)
    nbt.putDouble(EnergyTag, energy)
    nbt.putDouble(MaxEnergyTag, maxEnergy)
    nbt.putInt(TierTag, tier)
    if (!container.isEmpty) nbt.setNewCompoundTag(ContainerTag, container.save)
  }
}
