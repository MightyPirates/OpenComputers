package li.cil.oc.integration.opencomputers

import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.network.{EnvironmentHost, ManagedEnvironment}
import li.cil.oc.common.{Slot, Tier}
import li.cil.oc.server.component
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.RotationHelper
import li.cil.oc.{Constants, Settings, api}
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.RegistryKey
import net.minecraft.util.ResourceLocation
import net.minecraft.util.registry.Registry
import net.minecraft.world.server.ServerWorld
import net.minecraftforge.fml.server.ServerLifecycleHooks

/**
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
    if (host.world != null && !host.world.isClientSide) {
      if (stack.hasTag) {
        stack.getTag.getIntArray(Settings.namespace + "coord") match {
          case Array(x, y, z, side) => {
            val dimension = new ResourceLocation(stack.getTag.getString(Settings.namespace + "dimension"))
            ServerLifecycleHooks.getCurrentServer.getLevel(RegistryKey.create(Registry.DIMENSION_REGISTRY, dimension)) match {
              case world: ServerWorld => return new component.UpgradeMF(host, BlockPosition(x, y, z, world), Direction.from3DDataValue(side))
              case _ => // Invalid dimension ID
            }
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
