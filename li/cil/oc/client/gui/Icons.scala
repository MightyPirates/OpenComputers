package li.cil.oc.client.gui

import li.cil.oc.Config
import li.cil.oc.api.driver.Slot
import net.minecraft.util.Icon
import net.minecraftforge.client.event.TextureStitchEvent
import net.minecraftforge.event.ForgeSubscribe
import scala.collection.mutable

object Icons {
  val bySlotType = mutable.Map.empty[Slot.Value, Icon]

  @ForgeSubscribe
  def onItemIconRegister(e: TextureStitchEvent.Pre) {
    val iconRegister = e.map
    if (iconRegister.textureType == 1) {
      bySlotType += Slot.Card -> iconRegister.registerIcon(Config.resourceDomain + ":icon_card")
      bySlotType += Slot.HardDiskDrive -> iconRegister.registerIcon(Config.resourceDomain + ":icon_hdd")
      bySlotType += Slot.Memory -> iconRegister.registerIcon(Config.resourceDomain + ":icon_ram")
      bySlotType += Slot.Power -> iconRegister.registerIcon(Config.resourceDomain + ":icon_power")
    }
  }

}
