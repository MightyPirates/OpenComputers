package li.cil.oc.common.tileentity

import li.cil.oc.api
import li.cil.oc.api.network.Visibility
import li.cil.oc.common
import li.cil.oc.util.Color
import li.cil.oc.util.ItemColorizer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

class Cable extends traits.Environment with traits.NotAnalyzable with traits.ImmibisMicroblock with traits.Colored {
  val node = api.Network.newNode(this, Visibility.None).create()

  color = Color.LightGray

  def createItemStack() = {
    val stack = new ItemStack(Item.getItemFromBlock(getBlockType))
    if (color != Color.LightGray) {
      ItemColorizer.setColor(stack, color)
    }
    stack
  }

  def fromItemStack(stack: ItemStack): Unit = {
    if (ItemColorizer.hasColor(stack)) {
      color = ItemColorizer.getColor(stack)
    }
  }

  override def consumesDye = true

  override protected def onColorChanged() {
    super.onColorChanged()
    if (world != null && isServer) {
      api.Network.joinOrCreateNetwork(this)
    }
  }

  override def canUpdate = false

  override def getRenderBoundingBox = common.block.Cable.bounds(world, x, y, z).offset(x, y, z)
}
