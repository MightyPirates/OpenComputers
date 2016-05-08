package li.cil.oc.common.recipe

import li.cil.oc.util.Color
import li.cil.oc.util.ItemColorizer
import net.minecraft.block.Block
import net.minecraft.entity.passive.EntitySheep
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.world.World

/**
  * @author asie, Vexatos
  */
class ColorizeRecipe(target: Item, source: Array[Item] = null) extends ContainerItemAwareRecipe {
  def this(target: Block, source: Array[Item]) = this(Item.getItemFromBlock(target), source)
  def this(target: Block) = this(target, null)

  val targetItem = target
  val sourceItems = if (source != null) source else Array(targetItem)

  override def matches(crafting: InventoryCrafting, world: World): Boolean = {
    val stacks = (0 until crafting.getSizeInventory).flatMap(i => Option(crafting.getStackInSlot(i)))
    val targets = stacks.filter(stack => sourceItems.contains(stack.getItem) || stack.getItem == targetItem)
    val other = stacks.filterNot(targets.contains)
    targets.size == 1 && other.nonEmpty && other.forall(Color.isDye)
  }

  override def getCraftingResult(crafting: InventoryCrafting): ItemStack = {
    var targetStack: ItemStack = null
    val color = Array[Int](0, 0, 0)
    var colorCount = 0
    var maximum = 0

    (0 until crafting.getSizeInventory).flatMap(i => Option(crafting.getStackInSlot(i))).foreach { stack =>
      if (sourceItems.contains(stack.getItem)
        || stack.getItem == targetItem) {
        targetStack = stack.copy()
        targetStack.stackSize = 1
      } else {
        val dye = Color.findDye(stack)
        if (dye.isEmpty)
          return null

        val itemColor = EntitySheep.getDyeRgb(Color.byOreName(dye.get))
        val red = (itemColor(0) * 255.0F).toInt
        val green = (itemColor(1) * 255.0F).toInt
        val blue = (itemColor(2) * 255.0F).toInt
        maximum += Math.max(red, Math.max(green, blue))
        color(0) += red
        color(1) += green
        color(2) += blue
        colorCount = colorCount + 1
      }
    }

    if (targetStack == null) return null

    if (targetItem == targetStack.getItem) {
      if (ItemColorizer.hasColor(targetStack)) {
        val itemColor = ItemColorizer.getColor(targetStack)
        val red = (itemColor >> 16 & 255).toFloat / 255.0F
        val green = (itemColor >> 8 & 255).toFloat / 255.0F
        val blue = (itemColor & 255).toFloat / 255.0F
        maximum = (maximum.toFloat + Math.max(red, Math.max(green, blue)) * 255.0F).toInt
        color(0) = (color(0).toFloat + red * 255.0F).toInt
        color(1) = (color(1).toFloat + green * 255.0F).toInt
        color(2) = (color(2).toFloat + blue * 255.0F).toInt
        colorCount = colorCount + 1
      }
    } else if (sourceItems.contains(targetStack.getItem)) {
      targetStack = new ItemStack(targetItem, targetStack.stackSize, targetStack.getItemDamage)
    }

    var red = color(0) / colorCount
    var green = color(1) / colorCount
    var blue = color(2) / colorCount
    val max = maximum.toFloat / colorCount.toFloat
    val div = Math.max(red, Math.max(green, blue)).toFloat
    red = (red.toFloat * max / div).toInt
    green = (green.toFloat * max / div).toInt
    blue = (blue.toFloat * max / div).toInt
    ItemColorizer.setColor(targetStack, (red << 16) | (green << 8) | blue)
    targetStack
  }

  override def getRecipeSize = 10

  override def getRecipeOutput = null
}
