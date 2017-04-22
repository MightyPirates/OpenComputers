package li.cil.oc.common.recipe

import li.cil.oc.Settings
import li.cil.oc.common.Loot
import li.cil.oc.integration.util.Wrench
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.util.NonNullList
import net.minecraft.world.World

import scala.collection.immutable

class LootDiskCyclingRecipe extends IRecipe {
  override def matches(crafting: InventoryCrafting, world: World): Boolean = {
    val stacks = collectStacks(crafting).toArray
    stacks.count(!_.isEmpty) == 2 && stacks.exists(Loot.isLootDisk) && stacks.exists(Wrench.isWrench)
  }

  override def getCraftingResult(crafting: InventoryCrafting): ItemStack = {
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

  def getLootFactoryName(stack: ItemStack): String = stack.getTagCompound.getString(Settings.namespace + "lootFactory")

  def collectStacks(crafting: InventoryCrafting): immutable.IndexedSeq[ItemStack] = (0 until crafting.getSizeInventory).flatMap(i => Option(crafting.getStackInSlot(i)))

  override def getRecipeSize: Int = 2

  override def getRecipeOutput: ItemStack = ItemStack.EMPTY

  override def getRemainingItems(crafting: InventoryCrafting): NonNullList[ItemStack] = {
    val result = NonNullList.withSize[ItemStack](crafting.getSizeInventory, ItemStack.EMPTY)
    for (slot <- 0 until crafting.getSizeInventory) {
      val stack = crafting.getStackInSlot(slot)
      if (Wrench.isWrench(stack)) {
        result.set(slot, stack.copy())
        stack.setCount(0)
      }
    }
    result
  }
}
