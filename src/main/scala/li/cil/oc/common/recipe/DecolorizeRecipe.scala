package li.cil.oc.common.recipe

import li.cil.oc.util.ItemColorizer
import li.cil.oc.util.StackOption
import net.minecraft.block.Block
import net.minecraft.inventory.CraftingInventory
import net.minecraft.item.crafting.SpecialRecipe
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.item.ItemStack
import net.minecraft.util.IItemProvider
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World

/**
  * @author Vexatos
  */
class DecolorizeRecipe(id: ResourceLocation, target: IItemProvider) extends SpecialRecipe(id) {
  val targetItem: Item = target.asItem()

  override def matches(crafting: CraftingInventory, world: World): Boolean = {
    val stacks = (0 until crafting.getContainerSize).flatMap(i => StackOption(crafting.getItem(i)))
    val targets = stacks.filter(stack => stack.getItem == targetItem)
    val other = stacks.filterNot(targets.contains)
    targets.size == 1 && other.size == 1 && other.forall(_.getItem == Items.WATER_BUCKET)
  }

  override def assemble(crafting: CraftingInventory): ItemStack = {
    var targetStack: ItemStack = ItemStack.EMPTY

    (0 until crafting.getContainerSize).flatMap(i => StackOption(crafting.getItem(i))).foreach { stack =>
      if (stack.getItem == targetItem) {
        targetStack = stack.copy()
        targetStack.setCount(1)
      } else if (stack.getItem != Items.WATER_BUCKET) {
        return ItemStack.EMPTY
      }
    }

    if (targetStack.isEmpty) return ItemStack.EMPTY

    ItemColorizer.removeColor(targetStack)
    targetStack
  }

  override def canCraftInDimensions(width: Int, height: Int): Boolean = width * height >= 2

  override def getSerializer = RecipeSerializers.CRAFTING_DECOLORIZE
}
