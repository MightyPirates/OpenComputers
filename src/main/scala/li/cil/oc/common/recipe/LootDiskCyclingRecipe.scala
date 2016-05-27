package li.cil.oc.common.recipe

import li.cil.oc.Settings
import li.cil.oc.common.Loot
import li.cil.oc.integration.util.Wrench
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.world.World

class LootDiskCyclingRecipe extends IRecipe {
  override def matches(crafting: InventoryCrafting, world: World): Boolean = {
    val stacks = collectStacks(crafting).toArray
    stacks.length == 2 && stacks.exists(Loot.isLootDisk) && stacks.exists(Wrench.isWrench)
  }

  override def getCraftingResult(crafting: InventoryCrafting): ItemStack = {
    collectStacks(crafting).find(Loot.isLootDisk) match {
      case Some(lootDisk) =>
        val lootFactoryName = getLootFactoryName(lootDisk)
        val lootDiskStacks = Loot.worldDisks.map(_._1)
        val oldIndex = lootDiskStacks.indexWhere(s => getLootFactoryName(s) == lootFactoryName)
        val newIndex = (oldIndex + 1) % lootDiskStacks.length
        lootDiskStacks(newIndex).copy()
      case _ => null
    }
  }

  def getLootFactoryName(stack: ItemStack) = stack.getTagCompound.getString(Settings.namespace + "lootFactory")

  def collectStacks(crafting: InventoryCrafting) = (0 until crafting.getSizeInventory).flatMap(i => Option(crafting.getStackInSlot(i)))

  override def getRecipeSize: Int = 2

  override def getRecipeOutput: ItemStack = null

  override def getRemainingItems(crafting: InventoryCrafting): Array[ItemStack] = {
    val result = new Array[ItemStack](crafting.getSizeInventory)
    for (slot <- 0 until crafting.getSizeInventory) {
      val stack = crafting.getStackInSlot(slot)
      if (Wrench.isWrench(stack)) {
        result(slot) = stack.copy()
        stack.stackSize = 0
      }
    }
    result
  }
}
