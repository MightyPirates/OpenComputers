package li.cil.oc.server.driver.item

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.common.InventorySlots.Tier
import li.cil.oc.server.component.Container
import li.cil.oc.server.component.Container.{EntityContainer, TileEntityContainer}
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity

trait Item extends driver.Item {
  override def tier(stack: ItemStack) = Tier.One

  override def dataTag(stack: ItemStack) = Item.dataTag(stack)

  protected def isOneOf(stack: ItemStack, items: api.detail.ItemInfo*) = items.contains(api.Items.get(stack))

  final override def createEnvironment(stack: ItemStack, container: TileEntity) = createEnvironment(stack, TileEntityContainer(container))

  final override def createEnvironment(stack: ItemStack, container: Entity) = createEnvironment(stack, EntityContainer(container))

  protected def createEnvironment(stack: ItemStack, container: Container): ManagedEnvironment
}

object Item {
  def dataTag(stack: ItemStack) = {
    if (!stack.hasTagCompound) {
      stack.setTagCompound(new NBTTagCompound("tag"))
    }
    val nbt = stack.getTagCompound
    if (!nbt.hasKey(Settings.namespace + "data")) {
      nbt.setCompoundTag(Settings.namespace + "data", new NBTTagCompound())
    }
    nbt.getCompoundTag(Settings.namespace + "data")
  }
}