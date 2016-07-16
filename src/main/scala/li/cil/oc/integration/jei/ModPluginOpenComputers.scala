package li.cil.oc.integration.jei

import li.cil.oc.Settings
import li.cil.oc.integration.util.ItemBlacklist
import mezz.jei.api.IItemRegistry
import mezz.jei.api.IJeiHelpers
import mezz.jei.api.IJeiRuntime
import mezz.jei.api.IModPlugin
import mezz.jei.api.IModRegistry
import mezz.jei.api.IRecipeRegistry
import mezz.jei.api.JEIPlugin

@JEIPlugin
class ModPluginOpenComputers extends IModPlugin {
  override def onJeiHelpersAvailable(jeiHelpers: IJeiHelpers): Unit = {
    ItemBlacklist.consumers += jeiHelpers.getItemBlacklist.addItemToBlacklist
  }

  override def onItemRegistryAvailable(itemRegistry: IItemRegistry): Unit = {
  }

  override def register(registry: IModRegistry): Unit = {
    if (Settings.get.lootRecrafting) {
      registry.addRecipeHandlers(LootDiskCyclingRecipeHandler)
    }
    registry.addRecipeHandlers(ManualUsageHandler.ManualUsageRecipeHandler)
    registry.addRecipes(ManualUsageHandler.getRecipes(registry.getItemRegistry))
  }

  override def onRecipeRegistryAvailable(recipeRegistry: IRecipeRegistry): Unit = {
  }

  override def onRuntimeAvailable(jeiRuntime: IJeiRuntime): Unit = {
  }
}
