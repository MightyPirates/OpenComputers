package li.cil.oc.server.drivers

import li.cil.oc.Items
import li.cil.oc.api.ComponentType
import li.cil.oc.api.IItemDriver
import li.cil.oc.server.components.Disk
import net.minecraft.item.ItemStack
import li.cil.oc.api.Callback

object HDDDriver extends IItemDriver {
  def componentName = "disk"

  def apiName = "disk"

  def apiCode = null

  @Callback(name = "mount")
  def mount(component: Object, path: String) {

  }

  def componentType = ComponentType.HDD

  def itemType = new ItemStack(Items.hdd)

  def getComponent(item: ItemStack) = new HDDComponent(item)

  def close(component: Object) {
    component.asInstanceOf[HDDComponent].close()
  }
}

class HDDComponent(val item: ItemStack) {
  val disk = new Disk()
  
  def close() = disk.close()
}