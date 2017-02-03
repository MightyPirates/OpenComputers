package li.cil.oc.integration.opencomputers

import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.network.{EnvironmentHost, ManagedEnvironment}
import li.cil.oc.common.{Slot, Tier}
import li.cil.oc.server.component
import li.cil.oc.util.BlockPosition
import li.cil.oc.{Constants, Settings, api}
import net.minecraft.item.ItemStack
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.common.util.ForgeDirection

/**
  *
  * @author Vexatos
  */
object DriverUpgradeMF extends Item with HostAware {
  override def worksWith(stack: ItemStack): Boolean = isOneOf(stack,
    api.Items.get(Constants.ItemName.MFU))

  override def worksWith(stack: ItemStack, host: Class[_ <: EnvironmentHost]): Boolean =
    worksWith(stack) && isAdapter(host)

  override def slot(stack: ItemStack): String = Slot.Upgrade

  override def tier(stack: ItemStack) = Tier.Three

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost): ManagedEnvironment = {
    if (host.world != null && !host.world.isRemote) {
      if (stack.hasTagCompound) {
        stack.getTagCompound.getIntArray(Settings.namespace + "coord") match {
          case Array(x, y, z, dim, side) =>
            Option(DimensionManager.getWorld(dim)) match {
              case Some(world) => return new component.UpgradeMF(host, BlockPosition(x, y, z, world), ForgeDirection.getOrientation(side))
              case _ => // Invalid dimension ID
            }
          case _ => // Invalid tag
        }
      }
    }
    null
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (worksWith(stack))
        classOf[component.UpgradeMF]
      else null
  }

}
