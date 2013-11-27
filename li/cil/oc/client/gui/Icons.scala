package li.cil.oc.client.gui

import li.cil.oc.Settings
import li.cil.oc.api.driver.Slot
import net.minecraft.util.Icon
import net.minecraftforge.client.event.TextureStitchEvent
import net.minecraftforge.event.ForgeSubscribe
import scala.collection.mutable

object Icons {
  private val bySlotType = mutable.Map.empty[Slot, Icon]

  @ForgeSubscribe
  def onItemIconRegister(e: TextureStitchEvent.Pre) {
    val iconRegister = e.map
    if (iconRegister.textureType == 1) {
      bySlotType += Slot.Card -> iconRegister.registerIcon(Settings.resourceDomain + ":icon_card")
      bySlotType += Slot.Disk -> iconRegister.registerIcon(Settings.resourceDomain + ":icon_disk")
      bySlotType += Slot.HardDiskDrive -> iconRegister.registerIcon(Settings.resourceDomain + ":icon_hdd")
      bySlotType += Slot.Memory -> iconRegister.registerIcon(Settings.resourceDomain + ":icon_ram")
      bySlotType += Slot.Tool -> iconRegister.registerIcon(Settings.resourceDomain + ":icon_tool")
      bySlotType += Slot.Upgrade -> iconRegister.registerIcon(Settings.resourceDomain + ":icon_upgrade")
    }
  }

  def get(slotType: Slot) = bySlotType.get(slotType).orNull
}
