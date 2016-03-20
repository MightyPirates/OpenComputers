package li.cil.oc.common.recipe

import li.cil.oc.util.ItemColorizer
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

  val targetItem = target

  override def matches(crafting: InventoryCrafting, world: World): Boolean = {
    val stacks = (0 until crafting.getSizeInventory).flatMap(i => Option(crafting.getStackInSlot(i)))
    val targets = stacks.filter(stack => stack.getItem == targetItem)
    val other = stacks.filterNot(targets.contains)
    targets.size == 1 && other.size == 1 && other.forall(_.getItem == Items.water_bucket)
  }

  override def getCraftingResult(crafting: InventoryCrafting): ItemStack = {
    var targetStack: ItemStack = null

    (0 until crafting.getSizeInventory).flatMap(i => Option(crafting.getStackInSlot(i))).foreach { stack =>
      if (stack.getItem == targetItem) {
        targetStack = stack.copy()
        targetStack.stackSize = 1
      } else if (stack.getItem != Items.water_bucket) {
        return null
      }
    }

    if (targetStack == null) return null

    ItemColorizer.removeColor(targetStack)
    targetStack
  }

  override def getRecipeSize = 10

  override def getRecipeOutput = null
}
