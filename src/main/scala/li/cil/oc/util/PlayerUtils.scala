package li.cil.oc.util

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound

object PlayerUtils {
  def persistedData(player: EntityPlayer): NBTTagCompound = {
    val nbt = player.getEntityData
    if (!nbt.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
      nbt.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound())
    }
    nbt.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG)
  }
}
