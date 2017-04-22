package li.cil.oc.common.recipe

import li.cil.oc.util.ItemColorizer
import li.cil.oc.util.StackOption
import net.minecraft.block.Block
import net.minecraft.init.Items
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.world.World

/**
  * @author Vexatos
  */
class DecolorizeRecipe(target: Item) extends ContainerItemAwareRecipe {
  def this(target: Block) = this(Item.getItemFromBlock(target))

  val targetItem: Item = target

  override def matches(crafting: InventoryCrafting, world: World): Boolean = {
    val stacks = (0 until crafting.getSizeInventory).flatMap(i => StackOption(crafting.getStackInSlot(i)))
    val targets = stacks.filter(stack => stack.getItem == targetItem)
    val other = stacks.filterNot(targets.contains)
    targets.size == 1 && other.size == 1 && other.forall(_.getItem == Items.WATER_BUCKET)
  }

  override def getCraftingResult(crafting: InventoryCrafting): ItemStack = {
    var targetStack: ItemStack = ItemStack.EMPTY

    (0 until crafting.getSizeInventory).flatMap(i => StackOption(crafting.getStackInSlot(i))).foreach { stack =>
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

  override def getRecipeSize = 10

  override def getRecipeOutput = ItemStack.EMPTY
}
