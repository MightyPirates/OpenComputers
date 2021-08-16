package li.cil.oc.integration.opencomputers

import com.google.common.base.Strings
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.internal
import li.cil.oc.common.Tier
import li.cil.oc.server.driver.Registry
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

import scala.annotation.tailrec

trait Item extends driver.Item {
  def worksWith(stack: ItemStack, host: Class[_ <: EnvironmentHost]): Boolean =
    worksWith(stack) && !Registry.blacklist.exists {
      case (blacklistedStack, blacklistedHost) =>
        stack.isItemEqual(blacklistedStack) &&
          blacklistedHost.exists(_.isAssignableFrom(host))
    }

  override def tier(stack: ItemStack) = Tier.One

  override def dataTag(stack: ItemStack) = Item.dataTag(stack)

  protected def isOneOf(stack: ItemStack, items: api.detail.ItemInfo*) = items.filter(_ != null).contains(api.Items.get(stack))

  protected def isAdapter(host: Class[_ <: EnvironmentHost]) = classOf[internal.Adapter].isAssignableFrom(host)

  protected def isComputer(host: Class[_ <: EnvironmentHost]) = classOf[internal.Case].isAssignableFrom(host)

  protected def isRobot(host: Class[_ <: EnvironmentHost]) = classOf[internal.Robot].isAssignableFrom(host)

  protected def isRotatable(host: Class[_ <: EnvironmentHost]) = classOf[internal.Rotatable].isAssignableFrom(host)

  protected def isServer(host: Class[_ <: EnvironmentHost]) = classOf[internal.Server].isAssignableFrom(host)

  protected def isTablet(host: Class[_ <: EnvironmentHost]) = classOf[internal.Tablet].isAssignableFrom(host)

  protected def isMicrocontroller(host: Class[_ <: EnvironmentHost]) = classOf[internal.Microcontroller].isAssignableFrom(host)

  protected def isDrone(host: Class[_ <: EnvironmentHost]) = classOf[internal.Drone].isAssignableFrom(host)
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

  @tailrec
  private def getTag(tagCompound: NBTTagCompound, keys: Array[String]): Option[NBTTagCompound] = {
    if (keys.length == 0) Option(tagCompound)
    else if (!tagCompound.hasKey(keys(0))) None
    else getTag(tagCompound.getCompoundTag(keys(0)), keys.drop(1))
  }

  private def getTag(stack: ItemStack, keys: Array[String]): Option[NBTTagCompound] = {
    if (stack == null || stack.stackSize == 0) None
    else if (!stack.hasTagCompound) None
    else getTag(stack.getTagCompound, keys)
  }

  def address(stack: ItemStack): Option[String] = {
    val addressKey = "address"
    getTag(stack, Array(Settings.namespace + "data", "node")) match {
      case Some(tag) if tag.hasKey(addressKey) => Option(tag.getString(addressKey))
      case _ => None
    }
  }
}
