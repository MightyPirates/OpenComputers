package li.cil.oc.client.gui

import li.cil.oc.Settings
import li.cil.oc.api.driver.Slot
import net.minecraft.util.IIcon
import net.minecraftforge.client.event.TextureStitchEvent
import scala.collection.mutable
import li.cil.oc.common.InventorySlots.Tier
import cpw.mods.fml.common.eventhandler.SubscribeEvent

object Icons {
  private val bySlotType = mutable.Map.empty[Slot, IIcon]

  private val byTier = mutable.Map.empty[Int, IIcon]

  @SubscribeEvent
  def onItemIconRegister(e: TextureStitchEvent) {
    val iconRegister = e.map
    if (iconRegister.getTextureType == 1) {
      bySlotType += Slot.Card -> iconRegister.registerIcon(Settings.resourceDomain + ":icon_card")
      bySlotType += Slot.Disk -> iconRegister.registerIcon(Settings.resourceDomain + ":icon_disk")
      bySlotType += Slot.HardDiskDrive -> iconRegister.registerIcon(Settings.resourceDomain + ":icon_hdd")
      bySlotType += Slot.Memory -> iconRegister.registerIcon(Settings.resourceDomain + ":icon_ram")
      bySlotType += Slot.Processor -> iconRegister.registerIcon(Settings.resourceDomain + ":icon_cpu")
      bySlotType += Slot.Tool -> iconRegister.registerIcon(Settings.resourceDomain + ":icon_tool")
      bySlotType += Slot.Upgrade -> iconRegister.registerIcon(Settings.resourceDomain + ":icon_upgrade")
      bySlotType += Slot.UpgradeContainer -> iconRegister.registerIcon(Settings.resourceDomain + ":icon_upgrade_dynamic")

      byTier += Tier.None -> iconRegister.registerIcon(Settings.resourceDomain + ":icon_na")
      byTier += Tier.One -> iconRegister.registerIcon(Settings.resourceDomain + ":icon_tier0")
      byTier += Tier.Two -> iconRegister.registerIcon(Settings.resourceDomain + ":icon_tier1")
      byTier += Tier.Three -> iconRegister.registerIcon(Settings.resourceDomain + ":icon_tier2")
    }
  }

  def get(slotType: Slot) = bySlotType.get(slotType).orNull

  def get(tier: Int) = byTier.get(tier).orNull
}
