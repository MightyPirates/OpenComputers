package li.cil.oc.client.gui

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.Settings
import li.cil.oc.common.{Slot, Tier}
import net.minecraft.util.IIcon
import net.minecraftforge.client.event.TextureStitchEvent

import scala.collection.mutable

object Icons {
  private val bySlotType = mutable.Map.empty[String, IIcon]

  private val byTier = mutable.Map.empty[Int, IIcon]

  @SubscribeEvent
  def onItemIconRegister(e: TextureStitchEvent) {
    val iconRegister = e.map
    if (iconRegister.getTextureType == 1) {
      for (name <- Slot.All) {
        bySlotType += name -> iconRegister.registerIcon(Settings.resourceDomain + ":icons/" + name)
      }

      byTier += Tier.None -> iconRegister.registerIcon(Settings.resourceDomain + ":icons/na")
      for (tier <- Tier.One to Tier.Three) {
        byTier += tier -> iconRegister.registerIcon(Settings.resourceDomain + ":icons/tier" + tier)
    }
  }
  }

  def get(slotType: String) = bySlotType.get(slotType).orNull

  def get(tier: Int) = byTier.get(tier).orNull
}
