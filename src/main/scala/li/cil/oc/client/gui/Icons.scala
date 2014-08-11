package li.cil.oc.client.gui

import li.cil.oc.Settings
import li.cil.oc.api.driver.Slot
import li.cil.oc.common.Tier
import net.minecraft.util.Icon
import net.minecraftforge.client.event.TextureStitchEvent
import net.minecraftforge.event.ForgeSubscribe

import scala.collection.mutable

object Icons {
  private val bySlotType = mutable.Map.empty[Slot, Icon]

  private val byTier = mutable.Map.empty[Int, Icon]

  @ForgeSubscribe
  def onItemIconRegister(e: TextureStitchEvent.Pre) {
    val iconRegister = e.map
    if (iconRegister.textureType == 1) {
      bySlotType += Slot.Card -> iconRegister.registerIcon(Settings.resourceDomain + ":icons/card")
      bySlotType += Slot.Disk -> iconRegister.registerIcon(Settings.resourceDomain + ":icons/floppy")
      bySlotType += Slot.HardDiskDrive -> iconRegister.registerIcon(Settings.resourceDomain + ":icons/hdd")
      bySlotType += Slot.Memory -> iconRegister.registerIcon(Settings.resourceDomain + ":icons/memory")
      bySlotType += Slot.Processor -> iconRegister.registerIcon(Settings.resourceDomain + ":icons/cpu")
      bySlotType += Slot.Tool -> iconRegister.registerIcon(Settings.resourceDomain + ":icons/tool")
      bySlotType += Slot.Upgrade -> iconRegister.registerIcon(Settings.resourceDomain + ":icons/upgrade")
      bySlotType += Slot.UpgradeContainer -> iconRegister.registerIcon(Settings.resourceDomain + ":icons/container")

      byTier += Tier.None -> iconRegister.registerIcon(Settings.resourceDomain + ":icons/na")
      byTier += Tier.One -> iconRegister.registerIcon(Settings.resourceDomain + ":icons/tier0")
      byTier += Tier.Two -> iconRegister.registerIcon(Settings.resourceDomain + ":icons/tier1")
      byTier += Tier.Three -> iconRegister.registerIcon(Settings.resourceDomain + ":icons/tier2")
    }
  }

  def get(slotType: Slot) = bySlotType.get(slotType).orNull

  def get(tier: Int) = byTier.get(tier).orNull
}
