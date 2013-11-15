package li.cil.oc.common.tileentity

import li.cil.oc.api.driver
import li.cil.oc.api.network
import li.cil.oc.api.network.{ManagedEnvironment, Node}
import li.cil.oc.server.driver.Registry
import li.cil.oc.util.Persistable
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

trait ComponentInventory extends Inventory with network.Environment with Persistable {
  protected val components = Array.fill[Option[ManagedEnvironment]](getSizeInventory)(None)

  // ----------------------------------------------------------------------- //

  def installedMemory = items.foldLeft(0)((sum, stack) => sum + (stack match {
    case Some(item) => Registry.driverFor(item) match {
      case Some(driver: driver.Memory) => driver.amount(item)
      case _ => 0
    }
    case _ => 0
  }))

  // ----------------------------------------------------------------------- //

  abstract override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      for ((stack, slot) <- items.zipWithIndex collect {
        case (Some(stack), slot) => (stack, slot)
      } if components(slot).isEmpty) {
        components(slot) = Registry.driverFor(stack) match {
          case Some(driver) =>
            Option(driver.createEnvironment(stack, this)) match {
              case Some(environment) =>
                environment.load(driver.nbt(stack))
                Some(environment)
              case _ => None
            }
          case _ => None
        }
      }
      components collect {
        case Some(component) => node.connect(component.node)
      }
    }
  }

  abstract override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      components collect {
        case Some(component) => component.node.remove()
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def save(nbt: NBTTagCompound) = {
    items.zipWithIndex collect {
      case (Some(stack), slot) => (stack, slot)
    } foreach {
      case (stack, slot) => components(slot) match {
        case Some(environment) =>
          // We're guaranteed to have a driver for entries.
          environment.save(Registry.driverFor(stack).get.nbt(stack))
        case _ => // Nothing special to save.
      }
    }
    super.save(nbt) // Save items after updating their tags.
  }

  // ----------------------------------------------------------------------- //

  def getInventoryStackLimit = 1

  override protected def onItemAdded(slot: Int, item: ItemStack) = if (isServer) {
    Registry.driverFor(item) match {
      case Some(driver) => Option(driver.createEnvironment(item, this)) match {
        case Some(component) =>
          components(slot) = Some(component)
          component.load(driver.nbt(item))
          node.connect(component.node)
        case _ => // No environment (e.g. RAM).
      }
      case _ => // No driver.
    }
  }

  override protected def onItemRemoved(slot: Int, item: ItemStack) = if (isServer) {
    // Uninstall component previously in that slot.
    components(slot) match {
      case Some(component) =>
        // Note to self: we have to remove the node from the network *before*
        // saving, to allow file systems to close their handles before they
        // are saved (otherwise hard drives would restore all handles after
        // being installed into a different computer, even!)
        components(slot) = None
        component.node.remove()
        Registry.driverFor(item).foreach(driver =>
          component.save(driver.nbt(item)))
      case _ => // Nothing to do.
    }
  }
}