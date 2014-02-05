package li.cil.oc.server.driver

import li.cil.oc.api
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import scala.Some
import scala.collection.mutable.ArrayBuffer

/**
 * This class keeps track of registered drivers and provides installation logic
 * for each registered driver.
 *
 * Each component type must register its driver with this class to be used with
 * computers, since this class is used to determine whether an object is a
 * valid component or not.
 *
 * All drivers must be installed once the game starts - in the init phase - and
 * are then injected into all computers started up past that point. A driver is
 * a set of functions made available to the computer. These functions will
 * usually require a component of the type the driver wraps to be installed in
 * the computer, but may also provide context-free functions.
 */
private[oc] object Registry extends api.detail.DriverAPI {
  private val blocks = ArrayBuffer.empty[api.driver.Block]

  private val items = ArrayBuffer.empty[api.driver.Item]

  /** Used to keep track of whether we're past the init phase. */
  var locked = false

  def add(driver: api.driver.Block) {
    if (locked) throw new IllegalStateException("Please register all drivers in the init phase.")
    if (!blocks.contains(driver)) blocks += driver
  }

  def add(driver: api.driver.Item) {
    if (locked) throw new IllegalStateException("Please register all drivers in the init phase.")
    if (!blocks.contains(driver)) items += driver
  }

  def blockDriverFor(world: World, x: Int, y: Int, z: Int) =
    blocks.filter(_.worksWith(world, x, y, z)) match {
      case drivers if !drivers.isEmpty => Some(new CompoundBlockDriver(drivers.sortBy(_.getClass.getName): _*))
      case _ => None
    }

  def itemDriverFor(stack: ItemStack) =
    if (stack != null) items.find(_.worksWith(stack)) match {
      case None => None
      case Some(driver) => Some(driver)
    }
    else None
}
