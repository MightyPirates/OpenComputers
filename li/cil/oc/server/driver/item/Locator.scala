package li.cil.oc.server.driver.item

import li.cil.oc.{Settings, Items}
import li.cil.oc.api.driver.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.{TileEntity => MCTileEntity}
import li.cil.oc.server.driver.Registry

object Locator extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, Items.locator)

  override def createEnvironment(stack: ItemStack, container: MCTileEntity) ={
    val nbt = Registry.driverFor(stack) match {
      case Some(driver)=>driver.dataTag(stack)
      case _ => null
    }
    val x = if(nbt.hasKey(Settings.namespace +"xCenter")) nbt.getInteger(Settings.namespace +"xCenter")
    else container.xCoord
    val z = if(nbt.hasKey(Settings.namespace +"zCenter")) nbt.getInteger(Settings.namespace +"zCenter")
    else container.zCoord
    val scale = if(nbt.hasKey(Settings.namespace +"scale")) nbt.getInteger(Settings.namespace +"scale")
    else 512
    new component.Locator(container,x,z,scale)
  }

  override def slot(stack: ItemStack) = Slot.Upgrade
}
