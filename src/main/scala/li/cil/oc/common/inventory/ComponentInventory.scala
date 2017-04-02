package li.cil.oc.common.inventory

import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.api.driver.{DriverItem => ItemDriver}
import li.cil.oc.api.network
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.network.Node
import li.cil.oc.api.util.Lifecycle
import li.cil.oc.integration.opencomputers.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

trait ComponentInventory extends Inventory with network.Environment {
  lazy val components = Array.fill[Option[ManagedEnvironment]](getSizeInventory)(None)
  protected val updatingComponents = mutable.ArrayBuffer.empty[ManagedEnvironment]

  // ----------------------------------------------------------------------- //

  def host: EnvironmentHost

  // ----------------------------------------------------------------------- //

  def updateComponents() {
    if (updatingComponents.nonEmpty) {
      var i = 0
      // ArrayBuffer.foreach caches the size for performance reasons, but that
      // will cause issues if the list changed during iteration (e.g. because
      // a component removed itself / another component, such as the self-
      // destruct card from Computronics). Also, this list will generally be
      // quite short, so it won't have any noticeable impact, anyway.
      while (i < updatingComponents.size) {
        updatingComponents(i).update()
        i += 1
      }
    }
  }

  // ----------------------------------------------------------------------- //

  def connectComponents() {
    for (slot <- 0 until getSizeInventory if slot >= 0 && slot < components.length) {
      val stack = getStackInSlot(slot)
      if (!stack.isEmpty && components(slot).isEmpty && isComponentSlot(slot, stack)) {
        components(slot) = Option(Driver.driverFor(stack)) match {
          case Some(driver) =>
            Option(driver.createEnvironment(stack, host)) match {
              case Some(component) =>
                applyLifecycleState(component, Lifecycle.LifecycleState.Constructing)
                try {
                  component.load(dataTag(driver, stack))
                }
                catch {
                  case e: Throwable => OpenComputers.log.warn(s"An item component of type '${component.getClass.getName}' (provided by driver '${driver.getClass.getName}') threw an error while loading.", e)
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
    // Make sure our node is connected.
    api.Network.joinNewNetwork(node)
    components collect {
      case Some(component) =>
        applyLifecycleState(component, Lifecycle.LifecycleState.Initializing)
        connectItemNode(component.node)
        applyLifecycleState(component, Lifecycle.LifecycleState.Initialized)
    }
  }

  def disconnectComponents() {
    components collect {
      case Some(component) =>
        applyLifecycleState(component, Lifecycle.LifecycleState.Disposing)
        if (component.node != null) component.node.remove()
        applyLifecycleState(component, Lifecycle.LifecycleState.Disposed)
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
      if (!stack.isEmpty) {
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

  override protected def onItemAdded(slot: Int, stack: ItemStack) = if (isComponentSlot(slot, stack)) {
    Option(Driver.driverFor(stack)).foreach(driver =>
      Option(driver.createEnvironment(stack, host)) match {
        case Some(component) => this.synchronized {
          components(slot) = Some(component)
          applyLifecycleState(component, Lifecycle.LifecycleState.Constructing)
          try {
            component.load(dataTag(driver, stack))
          } catch {
            case e: Throwable => OpenComputers.log.warn(s"An item component of type '${component.getClass.getName}' (provided by driver '${driver.getClass.getName}') threw an error while loading.", e)
          }
          if (component.canUpdate) {
            assert(!updatingComponents.contains(component))
            updatingComponents += component
          }
          applyLifecycleState(component, Lifecycle.LifecycleState.Initializing)
          connectItemNode(component.node)
          applyLifecycleState(component, Lifecycle.LifecycleState.Initialized)
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
        applyLifecycleState(component, Lifecycle.LifecycleState.Disposing)
        Option(component.node).foreach(_.remove())
        Option(Driver.driverFor(stack)).foreach(save(component, _, stack))
        // However, nodes then may add themselves to a network again, to
        // ensure they have an address that gets sent to the client, used
        // for associating some components with each other. So we do it again.
        // TODO Should be possible to avoid this with lifecycle state now.
        Option(component.node).foreach(_.remove())
        applyLifecycleState(component, Lifecycle.LifecycleState.Disposed)
      }
      case _ => // Nothing to do.
    }
  }

  def isComponentSlot(slot: Int, stack: ItemStack) = true

  protected def connectItemNode(node: Node) {
    if (this.node != null && node != null) {
      this.node.connect(node)
    }
  }

  protected def dataTag(driver: ItemDriver, stack: ItemStack) =
    Option(driver.dataTag(stack)).getOrElse(Item.dataTag(stack))

  protected def save(component: ManagedEnvironment, driver: ItemDriver, stack: ItemStack): Unit = {
    try {
      val tag = dataTag(driver, stack)
      // Clear the tag compound before saving to get the same behavior as
      // in tile entities (otherwise entries have to be cleared manually).
      for (key <- tag.getKeySet.map(_.asInstanceOf[String])) {
        tag.removeTag(key)
      }
      component.save(tag)
    } catch {
      case e: Throwable => OpenComputers.log.warn(s"An item component of type '${component.getClass.getName}' (provided by driver '${driver.getClass.getName}') threw an error while saving.", e)
    }
  }

  protected def applyLifecycleState(component: AnyRef, state: Lifecycle.LifecycleState): Unit = component match {
    case lifecycle: Lifecycle => lifecycle.onLifecycleStateChange(state)
    case _ =>
  }
}
