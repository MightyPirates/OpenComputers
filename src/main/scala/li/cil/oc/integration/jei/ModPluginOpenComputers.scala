package li.cil.oc.integration.jei

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api.Items
import li.cil.oc.integration.util.ItemBlacklist
import li.cil.oc.integration.util.ItemSearch
import mezz.jei.api.IJeiRuntime
import mezz.jei.api.IModPlugin
import mezz.jei.api.IModRegistry
import mezz.jei.api.ISubtypeRegistry.ISubtypeInterpreter
import mezz.jei.api.JEIPlugin
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

@JEIPlugin
class ModPluginOpenComputers extends IModPlugin {
  override def register(registry: IModRegistry) {
    if (Settings.get.lootRecrafting) {
      registry.addRecipeHandlers(LootDiskCyclingRecipeHandler)
    }

    ItemBlacklist.hiddenItems.foreach(getter => registry.getJeiHelpers.getItemBlacklist.addItemToBlacklist(getter()))

    // This could go into the Description category, but Manual should always be in front of the Callback doc.
    ManualUsageHandler.ManualUsageRecipeCategory.initialize(registry.getJeiHelpers.getGuiHelper)
    registry.addRecipeCategories(ManualUsageHandler.ManualUsageRecipeCategory)
    registry.addRecipeHandlers(ManualUsageHandler.ManualUsageRecipeHandler)
    registry.addRecipes(ManualUsageHandler.getRecipes(registry))

    CallbackDocHandler.CallbackDocRecipeCategory.initialize(registry.getJeiHelpers.getGuiHelper)
    registry.addRecipeCategories(CallbackDocHandler.CallbackDocRecipeCategory)
    registry.addRecipeHandlers(CallbackDocHandler.CallbackDocRecipeHandler)
    registry.addRecipes(CallbackDocHandler.getRecipes(registry))

    registry.addAdvancedGuiHandlers(RelayGuiHandler)
  }

  private var stackUnderMouse: (GuiContainer, Int, Int) => Option[ItemStack] = null

  override def onRuntimeAvailable(jeiRuntime: IJeiRuntime) {
    if (stackUnderMouse == null) {
      ItemSearch.stackFocusing += ((container, mouseX, mouseY) => stackUnderMouse(container, mouseX, mouseY))
    }
    stackUnderMouse = (container, mouseX, mouseY) => Option(jeiRuntime.getItemListOverlay.getStackUnderMouse)

    ModJEI.runtime = Option(jeiRuntime)
  }
}
