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

  override def worksWith(stack: ItemStack, host: Class[_ <: EnvironmentHost]) = worksWith(stack)

  protected def isOneOf(stack: ItemStack, items: api.detail.ItemInfo*) = items.filter(_ != null).contains(api.Items.get(stack))

  protected def isRotatable(host: Class[_ <: EnvironmentHost]) = classOf[api.tileentity.Rotatable].isAssignableFrom(host)

  protected def isComputer(host: Class[_ <: EnvironmentHost]) = host.isInstanceOf[tileentity.traits.Computer] || host.isInstanceOf[tileentity.ServerRack]

  protected def isRobot(host: Class[_ <: EnvironmentHost]) = host.isInstanceOf[api.tileentity.Robot]

  protected def isTablet(host: Class[_ <: EnvironmentHost]) = host.isInstanceOf[item.TabletWrapper]
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
