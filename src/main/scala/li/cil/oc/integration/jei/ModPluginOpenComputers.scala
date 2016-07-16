package li.cil.oc.integration.jei

import li.cil.oc.Settings
import li.cil.oc.integration.util.ItemBlacklist
import li.cil.oc.integration.util.ItemSearch
import mezz.jei.api.IItemRegistry
import mezz.jei.api.IJeiHelpers
import mezz.jei.api.IJeiRuntime
import mezz.jei.api.IModPlugin
import mezz.jei.api.IModRegistry
import mezz.jei.api.IRecipeRegistry
import mezz.jei.api.JEIPlugin
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.item.ItemStack

@JEIPlugin
class ModPluginOpenComputers extends IModPlugin {

  override def onJeiHelpersAvailable(jeiHelpers: IJeiHelpers) {
    ItemBlacklist.consumers += jeiHelpers.getItemBlacklist.addItemToBlacklist
  }

  override def onItemRegistryAvailable(itemRegistry: IItemRegistry) {
  }

  override def register(registry: IModRegistry) {
    if (Settings.get.lootRecrafting) {
      registry.addRecipeHandlers(LootDiskCyclingRecipeHandler)
    }

    // This could go into the Description category, but Manual should always be in front of the Callback doc.
    ManualUsageHandler.ManualUsageRecipeCategory.initialize(registry.getJeiHelpers.getGuiHelper)
    registry.addRecipeCategories(ManualUsageHandler.ManualUsageRecipeCategory)
    registry.addRecipeHandlers(ManualUsageHandler.ManualUsageRecipeHandler)
    registry.addRecipes(ManualUsageHandler.getRecipes(registry))

    CallbackDocHandler.CallbackDocRecipeCategory.initialize(registry.getJeiHelpers.getGuiHelper)
    registry.addRecipeCategories(CallbackDocHandler.CallbackDocRecipeCategory)
    registry.addRecipeHandlers(CallbackDocHandler.CallbackDocRecipeHandler)
    registry.addRecipes(CallbackDocHandler.getRecipes(registry))
  }

  override def onRecipeRegistryAvailable(recipeRegistry: IRecipeRegistry) {
  }

  var stackUnderMouse: (GuiContainer, Int, Int) => Option[ItemStack] = null

  override def onRuntimeAvailable(jeiRuntime: IJeiRuntime) {
    if (stackUnderMouse == null) {
      ItemSearch.stackFocusing += ((container, mouseX, mouseY) => stackUnderMouse(container, mouseX, mouseY))
    }
    stackUnderMouse = (container, mouseX, mouseY) => Option(jeiRuntime.getItemListOverlay.getStackUnderMouse)

    // TODO check for focused input
  }
}
