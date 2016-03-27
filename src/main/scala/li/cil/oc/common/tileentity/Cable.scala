package li.cil.oc.common.tileentity

import li.cil.oc.api
import li.cil.oc.api.network.Visibility
import li.cil.oc.common
import li.cil.oc.Constants
import li.cil.oc.util.Color
import net.minecraft.item.EnumDyeColor
import li.cil.oc.util.ItemColorizer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

class Cable extends traits.Environment with traits.NotAnalyzable with traits.ImmibisMicroblock with traits.Colored {
  val node = api.Network.newNode(this, Visibility.None).create()

  setColor(Color.rgbValues(EnumDyeColor.SILVER))

  def createItemStack() = {
    val stack = api.Items.get(Constants.BlockName.Cable).createItemStack(1)
    if (getColor != Color.rgbValues(EnumDyeColor.SILVER)) {
      ItemColorizer.setColor(stack, getColor)
    }
    stack
  }

  def fromItemStack(stack: ItemStack): Unit = {
    if (ItemColorizer.hasColor(stack)) {
      setColor(ItemColorizer.getColor(stack))
    }
  }

  override def controlsConnectivity = true

  override def consumesDye = true

  override protected def onColorChanged() {
    super.onColorChanged()
    if (world != null && isServer) {
      api.Network.joinOrCreateNetwork(this)
    }
  }

  override def canUpdate = false

  override def getRenderBoundingBox = common.block.Cable.bounds(world, getPos).offset(x, y, z)
}
