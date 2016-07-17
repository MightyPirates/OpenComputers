package li.cil.oc.integration.nei

import java.util

import codechicken.nei.NEIClientConfig
import codechicken.nei.guihook.IContainerTooltipHandler
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.item.ItemStack
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.oredict.OreDictionary

class OredictTooltipHandler() extends IContainerTooltipHandler {
  override def handleTooltip(gui: GuiContainer, x: Int, y: Int, tooltip: util.List[String]) = tooltip

  override def handleItemDisplayName(gui: GuiContainer, stack: ItemStack, tooltip: util.List[String]) = tooltip

  override def handleItemTooltip(gui: GuiContainer, stack: ItemStack, x: Int, y: Int, tooltip: util.List[String]) = {
    if (NEIClientConfig.getBooleanSetting("inventory.oredict")) {
      for (oreId <- OreDictionary.getOreIDs(stack)) {
        tooltip.add(TextFormatting.DARK_GRAY + OreDictionary.getOreName(oreId))
      }
    }
    tooltip
  }
}
