package li.cil.oc.common.inventory

import java.util.logging.Level

import li.cil.oc.OpenComputers
import li.cil.oc.api.driver.{Container, Item => ItemDriver}
import li.cil.oc.api.{Driver, network}
import li.cil.oc.api.network.{ManagedEnvironment, Node}
import li.cil.oc.server.driver.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{NBTBase, NBTTagCompound}

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

trait ComponentInventory extends Inventory with network.Environment {
  lazy val components = Array.fill[Option[ManagedEnvironment]](getSizeInventory)(None)
  protected val updatingComponents = mutable.ArrayBuffer.empty[ManagedEnvironment]

  // ----------------------------------------------------------------------- //

  def componentContainer: Container

  // ----------------------------------------------------------------------- //

  def updateComponents() {
    if (updatingComponents.length > 0) {
      for (component <- updatingComponents) {
        component.update()
      }
    }
  }

  // ----------------------------------------------------------------------- //

  def connectComponents() {
    for (slot <- 0 until getSizeInventory if slot >= 0 && slot < components.length) {
      val stack = getStackInSlot(slot)
      if (stack != null && components(slot).isEmpty && isComponentSlot(slot)) {
        components(slot) = Option(Driver.driverFor(stack)) match {
          case Some(driver) =>
            Option(driver.createEnvironment(stack, componentContainer)) match {
              case Some(component) =>
                try {
                  component.load(dataTag(driver, stack))
                }
                catch {
                  case e: Throwable => OpenComputers.log.log(Level.WARNING, "An item component of type '%s' (provided by driver '%s') threw an error while loading.".format(component.getClass.getName, driver.getClass.getName), e)
                }
                if (component.canUpdate) {
                  assert(!updatingComponents.contains(component))
                  updatingComponents += component
                }
                Some(component)
              case _ => None
            }
          case _ => None
        }
      }
    }
    components collect {
      case Some(component) => connectItemNode(component.node)
    }
  }

  def disconnectComponents() {
    components collect {
      case Some(component) if component.node != null => component.node.remove()
    }
  }

  // ----------------------------------------------------------------------- //

  override def save(nbt: NBTTagCompound) = {
    saveComponents()
    super.save(nbt) // Save items after updating their tags.
  }

  def saveComponents() {
    for (slot <- 0 until getSizeInventory) {
      val stack = getStackInSlot(slot)
      if (stack != null) {
        components(slot) match {
          case Some(component) =>
            // We're guaranteed to have a driver for entries.
            save(component, Driver.driverFor(stack), stack)
          case _ => // Nothing special to save.
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def getInventoryStackLimit = 1

  override protected def onItemAdded(slot: Int, stack: ItemStack) = if (isComponentSlot(slot)) {
    Option(Driver.driverFor(stack)).foreach(driver =>
      Option(driver.createEnvironment(stack, componentContainer)) match {
        case Some(component) => this.synchronized {
          components(slot) = Some(component)
          try {
            component.load(dataTag(driver, stack))
          } catch {
            case e: Throwable => OpenComputers.log.log(Level.WARNING, "An item component of type '%s' (provided by driver '%s') threw an error while loading.".format(component.getClass.getName, driver.getClass.getName), e)
          }
          connectItemNode(component.node)
          if (component.canUpdate) {
            assert(!updatingComponents.contains(component))
            updatingComponents += component
          }
          save(component, driver, stack)
        }
        case _ => // No environment (e.g. RAM).
      })
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    // Uninstall component previously in that slot.
    components(slot) match {
      case Some(component) => this.synchronized {
        // Note to self: we have to remove the node from the network *before*
        // saving, to allow file systems to close their handles before they
        // are saved (otherwise hard drives would restore all handles after
        // being installed into a different computer, even!)
        components(slot) = None
        updatingComponents -= component
        Option(component.node).foreach(_.remove())
        Option(Driver.driverFor(stack)).foreach(save(component, _, stack))
        // However, nodes then may add themselves to a network again, to
        // ensure they have an address that gets sent to the client, used
        // for associating some components with each other. So we do it again.
        Option(component.node).foreach(_.remove())
      }
      case _ => // Nothing to do.
    }
  }

  def isComponentSlot(slot: Int) = true

  protected def connectItemNode(node: Node) {
    if (this.node != null && node != null) {
      this.node.connect(node)
    }
  }

  protected def dataTag(driver: ItemDriver, stack: ItemStack) =
    Option(driver.dataTag(stack)).getOrElse(Item.dataTag(stack))

  protected def save(component: ManagedEnvironment, driver: ItemDriver, stack: ItemStack) {
    try {
      val tag = dataTag(driver, stack)
      // Clear the tag compound before saving to get the same behavior as
      // in tile entities (otherwise entries have to be cleared manually).
      for (key <- tag.getTags.map(_.asInstanceOf[NBTBase].getName)) {
        tag.removeTag(key)
      }
      component.save(tag)
    } catch {
      case e: Throwable => OpenComputers.log.log(Level.WARNING, "An item component of type '%s' (provided by driver '%s') threw an error while saving.".format(component.getClass.getName, driver.getClass.getName), e)
    }
  }
}
