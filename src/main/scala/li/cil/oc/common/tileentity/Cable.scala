package li.cil.oc.common.tileentity

import li.cil.oc.api
import li.cil.oc.api.network.Visibility
import li.cil.oc.common
import li.cil.oc.Constants
import li.cil.oc.util.Color
import net.minecraft.item.DyeColor
import li.cil.oc.util.ItemColorizer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.tileentity.TileEntityType

class Cable(selfType: TileEntityType[_ <: Cable]) extends TileEntity(selfType) with traits.Environment with traits.NotAnalyzable with traits.ImmibisMicroblock with traits.Colored {
  val node = api.Network.newNode(this, Visibility.None).create()

  setColor(Color.rgbValues(DyeColor.LIGHT_GRAY))

  def createItemStack() = {
    val stack = api.Items.get(Constants.BlockName.Cable).createItemStack(1)
    if (getColor != Color.rgbValues(DyeColor.LIGHT_GRAY)) {
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
    if (getLevel != null && isServer) {
      api.Network.joinOrCreateNetwork(this)
    }
  }
}
