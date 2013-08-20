package li.cil.oc.server.computer

import scala.collection.mutable.Map
import li.cil.oc.api.IBlockDriver
import li.cil.oc.api.IItemDriver
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import com.naef.jnlua.LuaState

/**
 * This class keeps track of registered drivers and provides installation logic
 * for each registered component type.
 *
 * Each component type must register its driver with this class to be used with
 * computers, since this class is used to determine whether an object is a
 * valid component or not.
 */
object Drivers {
  private val blocks = Map.empty[Int, Driver]
  private val items = Map.empty[Int, Driver]

  /**
   * Registers a new driver for a block component.
   *
   * Whenever the neighboring blocks of a computer change, it checks if there
   * exists a driver for the changed block, and if so adds it to the list of
   * available components.
   *
   * @param driver the driver for that block type.
   */
  def addDriver(driver: IBlockDriver) {
    if (blocks.contains(driver.blockType.blockID)) return
    blocks += driver.blockType.blockID -> new Driver(driver)
  }

  /**
   * Registers a new driver for an item component.
   *
   * Item components can inserted into a computers component slots. They have
   * to specify their type, to determine into which slots they can fit.
   *
   * @param driver the driver for that item type.
   */
  def addDriver(driver: IItemDriver) {
    if (items.contains(driver.itemType.itemID)) return
    items += driver.itemType.itemID -> new Driver(driver)
  }

  def getDriver(block: Block) = blocks(block.blockID)

  def getDriver(item: ItemStack) = blocks(item.itemID)

  def injectInto(context: IComputerContext) = (blocks.values ++ items.values).foreach(_.injectInto(context))
}
