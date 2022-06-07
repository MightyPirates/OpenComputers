package li.cil.oc.integration.jei

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api.Items
import li.cil.oc.common.recipe.LootDiskCyclingRecipe
import li.cil.oc.integration.jei.CallbackDocHandler.CallbackDocRecipe
import li.cil.oc.integration.jei.ManualUsageHandler.ManualUsageRecipe
import li.cil.oc.integration.util.ItemBlacklist
import li.cil.oc.integration.util.ItemSearch
import li.cil.oc.util.StackOption
import mezz.jei.api.IJeiRuntime
import mezz.jei.api.IModPlugin
import mezz.jei.api.IModRegistry
import mezz.jei.api.ISubtypeRegistry
import mezz.jei.api.ISubtypeRegistry.ISubtypeInterpreter
import mezz.jei.api.JEIPlugin
import mezz.jei.api.ingredients.IModIngredientRegistration
import mezz.jei.api.recipe.{IRecipeCategoryRegistration, VanillaRecipeCategoryUid}
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

@JEIPlugin
class ModPluginOpenComputers extends IModPlugin {
  override def registerCategories(registry: IRecipeCategoryRegistration): Unit = {
    registry.addRecipeCategories(ManualUsageHandler.ManualUsageRecipeCategory)
    registry.addRecipeCategories(CallbackDocHandler.CallbackDocRecipeCategory)

  }

  override def register(registry: IModRegistry) {
    if (Settings.get.lootRecrafting) {
      registry.handleRecipes(classOf[LootDiskCyclingRecipe], LootDiskCyclingRecipeHandler, VanillaRecipeCategoryUid.CRAFTING)
    }

    ItemBlacklist.hiddenItems.foreach(getter => registry.getJeiHelpers.getIngredientBlacklist.addIngredientToBlacklist(getter()))

    // This could go into the Description category, but Manual should always be in front of the Callback doc.
    ManualUsageHandler.ManualUsageRecipeCategory.initialize(registry.getJeiHelpers.getGuiHelper)
    CallbackDocHandler.CallbackDocRecipeCategory.initialize(registry.getJeiHelpers.getGuiHelper)

    registry.handleRecipes(classOf[ManualUsageRecipe], ManualUsageHandler.ManualUsageRecipeHandler, ManualUsageHandler.ManualUsageRecipeCategory.getUid)
    registry.handleRecipes(classOf[CallbackDocRecipe], CallbackDocHandler.CallbackDocRecipeHandler, CallbackDocHandler.CallbackDocRecipeCategory.getUid)

    registry.addRecipes(ManualUsageHandler.getRecipes(registry), ManualUsageHandler.ManualUsageRecipeCategory.getUid)
    registry.addRecipes(CallbackDocHandler.getRecipes(registry), CallbackDocHandler.CallbackDocRecipeCategory.getUid)

    registry.addAdvancedGuiHandlers(RelayGuiHandler)

    ModJEI.ingredientRegistry = Option(registry.getIngredientRegistry)
  }

  private var stackUnderMouse: (GuiContainer, Int, Int) => StackOption = _

  override def onRuntimeAvailable(jeiRuntime: IJeiRuntime) {
    if (stackUnderMouse == null) {
      ItemSearch.stackFocusing += ((container, mouseX, mouseY) => stackUnderMouse(container, mouseX, mouseY))
    }
    stackUnderMouse = (container, mouseX, mouseY) => StackOption(jeiRuntime.getItemListOverlay.getStackUnderMouse)

    ModJEI.runtime = Option(jeiRuntime)
  }

  override def registerIngredients(registry: IModIngredientRegistration) {
  }

  override def registerItemSubtypes(subtypeRegistry: ISubtypeRegistry) {
    def useNBT(names: String*) = names.map(name => {
      val info = Items.get(name)
      Option(info.item).getOrElse(Item.getItemFromBlock(info.block))
    }).filter(_ != null).distinct.foreach(subtypeRegistry.useNbtForSubtypes(_))

    // Only the preconfigured blocks and items have to be here.
    useNBT(
      Constants.BlockName.Microcontroller,
      Constants.BlockName.Robot,

      Constants.ItemName.Drone,
      Constants.ItemName.Tablet
    )

    subtypeRegistry.registerSubtypeInterpreter(Items.get(Constants.ItemName.Floppy).item(), new ISubtypeInterpreter {
      override def apply(stack: ItemStack): String = {
        if (!stack.hasTagCompound) return null
        val compound: NBTTagCompound = stack.getTagCompound
        val data = new NBTTagCompound
        // Separate loot disks from normal floppies
        if (compound.hasKey(Settings.namespace + "lootFactory")) {
          data.setTag(Settings.namespace + "lootFactory", compound.getTag(Settings.namespace + "lootFactory"))
        }
        if (data.isEmpty) null else data.toString
      }
    })
  }
}
