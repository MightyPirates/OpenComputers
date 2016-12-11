package li.cil.oc.integration.jei

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api.Items
import li.cil.oc.integration.util.ItemBlacklist
import li.cil.oc.integration.util.ItemSearch
import mezz.jei.api.IJeiRuntime
import mezz.jei.api.IModPlugin
import mezz.jei.api.IModRegistry
import mezz.jei.api.ISubtypeRegistry
import mezz.jei.api.ISubtypeRegistry.ISubtypeInterpreter
import mezz.jei.api.JEIPlugin
import mezz.jei.api.ingredients.IModIngredientRegistration
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

  private var stackUnderMouse: (GuiContainer, Int, Int) => Option[ItemStack] = _

  override def onRuntimeAvailable(jeiRuntime: IJeiRuntime) {
    if (stackUnderMouse == null) {
      ItemSearch.stackFocusing += ((container, mouseX, mouseY) => stackUnderMouse(container, mouseX, mouseY))
    }
    stackUnderMouse = (container, mouseX, mouseY) => Option(jeiRuntime.getItemListOverlay.getStackUnderMouse)

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

    subtypeRegistry.registerNbtInterpreter(Items.get(Constants.ItemName.Floppy).item(), new ISubtypeInterpreter {
      override def getSubtypeInfo(stack: ItemStack): String = {
        if (!stack.hasTagCompound) return null
        val compound: NBTTagCompound = stack.getTagCompound
        val data = new NBTTagCompound
        // Separate loot disks from normal floppies
        if (compound.hasKey(Settings.namespace + "lootFactory")) {
          data.setTag(Settings.namespace + "lootFactory", compound.getTag(Settings.namespace + "lootFactory"))
        }
        if (data.hasNoTags) null else data.toString
      }
    })
  }
}
