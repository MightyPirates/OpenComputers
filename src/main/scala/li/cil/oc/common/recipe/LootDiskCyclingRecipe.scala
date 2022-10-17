package li.cil.oc.common.recipe

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.Loot
import li.cil.oc.integration.util.Wrench
import li.cil.oc.util.StackOption
import net.minecraft.inventory.CraftingInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.Ingredient
import net.minecraft.item.crafting.ICraftingRecipe
import net.minecraft.util.NonNullList
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World

import scala.collection.JavaConverters
import scala.collection.immutable

class LootDiskCyclingRecipe(val getId: ResourceLocation) extends ICraftingRecipe {
  val ingredients = NonNullList.create[Ingredient]
  ingredients.add(Ingredient.of(Loot.disksForCycling.toArray: _*))
  ingredients.add(Ingredient.of(api.Items.get(Constants.ItemName.Wrench).createItemStack(1)))

  override def matches(crafting: CraftingInventory, world: World): Boolean = {
    val stacks = collectStacks(crafting).toArray
    stacks.length == 2 && stacks.exists(Loot.isLootDisk) && stacks.exists(Wrench.isWrench)
  }

  override def assemble(crafting: CraftingInventory): ItemStack = {
    val lootDiskStacks = Loot.disksForCycling
    collectStacks(crafting).find(Loot.isLootDisk) match {
      case Some(lootDisk) if lootDiskStacks.nonEmpty =>
        val lootFactoryName = getLootFactoryName(lootDisk)
        val oldIndex = lootDiskStacks.indexWhere(s => getLootFactoryName(s) == lootFactoryName)
        val newIndex = (oldIndex + 1) % lootDiskStacks.length
        lootDiskStacks(newIndex).copy()
      case _ => ItemStack.EMPTY
    }
  }

  def getLootFactoryName(stack: ItemStack): String = stack.getTag.getString(Settings.namespace + "lootFactory")

  def collectStacks(crafting: CraftingInventory): immutable.IndexedSeq[ItemStack] = (0 until crafting.getContainerSize).flatMap(i => StackOption(crafting.getItem(i)))

  override def canCraftInDimensions(width: Int, height: Int): Boolean = width * height >= 2

  override def getResultItem = Loot.disksForCycling.headOption match {
    case Some(lootDisk) => lootDisk
    case _ => ItemStack.EMPTY
  }

  override def getRemainingItems(crafting: CraftingInventory): NonNullList[ItemStack] = {
    val result = NonNullList.withSize[ItemStack](crafting.getContainerSize, ItemStack.EMPTY)
    for (slot <- 0 until crafting.getContainerSize) {
      val stack = crafting.getItem(slot)
      if (Wrench.isWrench(stack)) {
        result.set(slot, stack.copy())
        stack.setCount(0)
      }
    }
    result
  }

  override def getIngredients = ingredients

  override def getSerializer = RecipeSerializers.CRAFTING_LOOTDISK_CYCLING
}
