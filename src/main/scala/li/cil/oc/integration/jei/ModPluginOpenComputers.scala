package li.cil.oc.integration.jei

import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.Items
import li.cil.oc.client.gui.Relay
import li.cil.oc.integration.jei.CallbackDocHandler.CallbackDocRecipe
import li.cil.oc.integration.jei.ManualUsageHandler.ManualUsageRecipe
import li.cil.oc.integration.util.ItemBlacklist
import li.cil.oc.integration.util.ItemSearch
import li.cil.oc.util.StackOption
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter
import mezz.jei.api.ingredients.subtypes.UidContext
import mezz.jei.api.registration.IAdvancedRegistration
import mezz.jei.api.registration.IGuiHandlerRegistration
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import mezz.jei.api.registration.ISubtypeRegistration
import mezz.jei.api.runtime.IIngredientManager
import mezz.jei.api.runtime.IJeiRuntime
import net.minecraft.client.gui.screen.inventory.ContainerScreen
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.ResourceLocation

import scala.collection.JavaConverters._

@JeiPlugin
class ModPluginOpenComputers extends IModPlugin {
  override def getPluginUid = new ResourceLocation(OpenComputers.ID, "jei_plugin")

  override def registerCategories(registry: IRecipeCategoryRegistration): Unit = {
    registry.addRecipeCategories(ManualUsageHandler.ManualUsageRecipeCategory)
    registry.addRecipeCategories(CallbackDocHandler.CallbackDocRecipeCategory)
  }

  override def registerRecipes(registration: IRecipeRegistration) {
    registration.addRecipes(ManualUsageHandler.getRecipes(registration), ManualUsageHandler.ManualUsageRecipeCategory.getUid)
    registration.addRecipes(CallbackDocHandler.getRecipes(registration), CallbackDocHandler.CallbackDocRecipeCategory.getUid)
  }

  override def registerGuiHandlers(registration: IGuiHandlerRegistration) = {
    registration.addGuiContainerHandler(classOf[Relay], RelayGuiHandler)
  }

  override def registerAdvanced(registration: IAdvancedRegistration) = {
    // This could go into the Description category, but Manual should always be in front of the Callback doc.
    ManualUsageHandler.ManualUsageRecipeCategory.initialize(registration.getJeiHelpers.getGuiHelper)
    CallbackDocHandler.CallbackDocRecipeCategory.initialize(registration.getJeiHelpers.getGuiHelper)
  }

  private var stackUnderMouse: (ContainerScreen[_], Int, Int) => StackOption = _

  override def onRuntimeAvailable(jeiRuntime: IJeiRuntime) {
    if (stackUnderMouse == null) {
      ItemSearch.stackFocusing += ((container, mouseX, mouseY) => stackUnderMouse(container, mouseX, mouseY))
    }
    stackUnderMouse = (container, mouseX, mouseY) => StackOption(jeiRuntime.getIngredientListOverlay.getIngredientUnderMouse(VanillaTypes.ITEM))

    jeiRuntime.getIngredientManager.removeIngredientsAtRuntime(VanillaTypes.ITEM, ItemBlacklist.hiddenItems.map(getter => getter()).asJavaCollection)

    ModJEI.runtime = Option(jeiRuntime)
    ModJEI.ingredientRegistry = Option(jeiRuntime.getIngredientManager)
  }

  override def registerItemSubtypes(subtypeRegistry: ISubtypeRegistration) {
    def useNBT(names: String*) = names.map(name => {
      val info = Items.get(name)
      Option(info.item).getOrElse(info.block.asItem())
    }).filter(_ != null).distinct.foreach(subtypeRegistry.useNbtForSubtypes(_))

    // Only the preconfigured blocks and items have to be here.
    useNBT(
      Constants.BlockName.Microcontroller,
      Constants.BlockName.Robot,

      Constants.ItemName.Drone,
      Constants.ItemName.Tablet
    )

    subtypeRegistry.registerSubtypeInterpreter(Items.get(Constants.ItemName.Floppy).item(), new IIngredientSubtypeInterpreter[ItemStack] {
      override def apply(stack: ItemStack, ctx: UidContext): String = {
        if (!stack.hasTag) return IIngredientSubtypeInterpreter.NONE
        // Separate loot disks from normal floppies
        Option(stack.getTag.get(Settings.namespace + "lootFactory")) match {
          case Some(lf) => lf.toString
          case None => IIngredientSubtypeInterpreter.NONE
        }
      }
    })
  }
}
