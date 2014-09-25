package li.cil.oc.server.driver.item

import li.cil.oc.api.driver
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.common.{Tier, item, tileentity}
import li.cil.oc.{Settings, api}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

trait Item extends driver.Item {
  override def tier(stack: ItemStack) = Tier.One

  override def dataTag(stack: ItemStack) = Item.dataTag(stack)

  override def worksWith(stack: ItemStack, host: EnvironmentHost) = worksWith(stack)

  protected def isOneOf(stack: ItemStack, items: api.detail.ItemInfo*) = items.filter(_ != null).contains(api.Items.get(stack))

  protected def isComputer(host: EnvironmentHost) = host.isInstanceOf[tileentity.traits.Computer] || host.isInstanceOf[tileentity.ServerRack]

  protected def isRobot(host: EnvironmentHost) = host.isInstanceOf[api.tileentity.Robot]

  protected def isTablet(host: EnvironmentHost) = host.isInstanceOf[item.TabletWrapper]
}

object Item {
  def dataTag(stack: ItemStack) = {
    if (!stack.hasTagCompound) {
      stack.setTagCompound(new NBTTagCompound())
    }
    val nbt = stack.getTagCompound
    if (!nbt.hasKey(Settings.namespace + "data")) {
      nbt.setTag(Settings.namespace + "data", new NBTTagCompound())
    }
    nbt.getCompoundTag(Settings.namespace + "data")
  }
}
