package li.cil.oc.integration.opencomputers

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver
import li.cil.oc.api.driver.DriverItem
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.internal
import li.cil.oc.api.tileentity.Rotatable
import li.cil.oc.api.util.Location
import li.cil.oc.common.Tier
import li.cil.oc.server.driver.Registry
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

trait Item extends DriverItem {
  def worksWith(stack: ItemStack, host: Class[_ <: Location]): Boolean =
    worksWith(stack) && !Registry.blacklist.exists {
      case (blacklistedStack, blacklistedHost) =>
        stack.isItemEqual(blacklistedStack) &&
          blacklistedHost.exists(_.isAssignableFrom(host))
    }

  override def tier(stack: ItemStack) = Tier.One

  override def dataTag(stack: ItemStack) = Item.dataTag(stack)

  protected def isOneOf(stack: ItemStack, items: api.detail.ItemInfo*) = items.filter(_ != null).contains(api.Items.get(stack))

  protected def isAdapter(host: Class[_ <: Location]) = classOf[internal.Adapter].isAssignableFrom(host)

  protected def isComputer(host: Class[_ <: Location]) = classOf[internal.Case].isAssignableFrom(host)

  protected def isRobot(host: Class[_ <: Location]) = classOf[internal.Robot].isAssignableFrom(host)

  protected def isRotatable(host: Class[_ <: Location]) = classOf[Rotatable].isAssignableFrom(host)

  protected def isServer(host: Class[_ <: Location]) = classOf[internal.Server].isAssignableFrom(host)

  protected def isTablet(host: Class[_ <: Location]) = classOf[internal.Tablet].isAssignableFrom(host)

  protected def isMicrocontroller(host: Class[_ <: Location]) = classOf[internal.Microcontroller].isAssignableFrom(host)

  protected def isDrone(host: Class[_ <: Location]) = classOf[internal.Drone].isAssignableFrom(host)
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
