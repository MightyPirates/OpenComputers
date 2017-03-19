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

  private final val ItemsTag = Constants.namespace + "items"
  private final val SlotTag = "slot"
  private final val ItemTag = "item"
  private final val IsRunningTag = Constants.namespace + "isRunning"
  private final val EnergyTag = Constants.namespace + "energy"
  private final val MaxEnergyTag = Constants.namespace + "maxEnergy"
  private final val TierTag = Constants.namespace + "tier"
  private final val ContainerTag = Constants.namespace + "container"

  override def load(nbt: NBTTagCompound) {
    nbt.getTagList(ItemsTag, NBT.TAG_COMPOUND).foreach((slotNbt: NBTTagCompound) => {
      val slot = slotNbt.getByte(SlotTag)
      if (slot >= 0 && slot < items.length) {
        items(slot) = Option(new ItemStack(slotNbt.getCompoundTag(ItemTag)))
      }
    })
    isRunning = nbt.getBoolean(IsRunningTag)
    energy = nbt.getDouble(EnergyTag)
    maxEnergy = nbt.getDouble(MaxEnergyTag)
    tier = nbt.getInteger(TierTag)
    if (nbt.hasKey(ContainerTag)) {
      container = Option(new ItemStack(nbt.getCompoundTag(ContainerTag)))
    }
  }

  override def save(nbt: NBTTagCompound) {
    nbt.setNewTagList(ItemsTag,
      items.zipWithIndex collect {
        case (Some(stack), slot) => (stack, slot)
      } map {
        case (stack, slot) =>
          val slotNbt = new NBTTagCompound()
          slotNbt.setByte(SlotTag, slot.toByte)
          slotNbt.setNewCompoundTag(ItemTag, stack.writeToNBT)
      })
    nbt.setBoolean(IsRunningTag, isRunning)
    nbt.setDouble(EnergyTag, energy)
    nbt.setDouble(MaxEnergyTag, maxEnergy)
    nbt.setInteger(TierTag, tier)
    container.foreach(stack => nbt.setNewCompoundTag(ContainerTag, stack.writeToNBT))
  }
}
