package li.cil.oc.integration.buildcraft.recipes

import java.util

import buildcraft.api.recipes.BuildcraftRecipeRegistry
import buildcraft.api.recipes.IProgrammingRecipe
import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.Loot
import li.cil.oc.integration.util.Power
import net.minecraft.item.ItemStack

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

object LootDiskProgrammableRecipe extends IProgrammingRecipe {
  def register() = BuildcraftRecipeRegistry.programmingTable.addRecipe(LootDiskProgrammableRecipe)

  override def getId: String = OpenComputers.ID + ":loot_disk"

  override def getOptions(width: Int, height: Int): util.List[ItemStack] = {
    val options = mutable.ArrayBuffer.empty[ItemStack]
    options.sizeHint(width * height)

    for (stack <- Loot.disksForCycling) {
      options += stack.copy()
    }

    options
  }

  override def getEnergyCost(option: ItemStack): Int = Power.toRF(Settings.get.costProgrammingTable)

  override def canCraft(input: ItemStack): Boolean = api.Items.get(input) == api.Items.get(Constants.ItemName.Floppy)

  override def craft(input: ItemStack, option: ItemStack): ItemStack = option.copy()
}
