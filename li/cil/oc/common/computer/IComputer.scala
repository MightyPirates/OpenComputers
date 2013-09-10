package li.cil.oc.common.computer
import li.cil.oc.api.IBlockDriver
import li.cil.oc.api.IItemDriver
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

/**
 * This interface is used to be able to use the same basic type for storing a
 * computer on both client and server. There are two implementations of this,
 * one for the server, which does hold the actual computer logic, and one for
 * the client, which does nothing at all.
 */
trait IComputer {
  /**
   * Tries to add the specified item as a component to the computer.
   *
   * This can fail if there's either no driver for that item type, or another
   * component with that ID is already installed in the computer. It returns
   * the driver used for that item, to allow further checks (such as whether
   * the slot the item should be installed into is valid based on the component
   * type specified in the driver).
   *
   * This will add the component and driver to the list of installed components
   * and send the install signal to the computer.
   */
  def add(item: ItemStack, id: Int): Option[IItemDriver]

  /**
   * Tries to add the specified block as a component to the computer.
   *
   * This can fail if there's either no driver for that block type, or another
   * component with that ID is already installed in the computer. It returns
   * the driver used for that block, to allow further checks.
   *
   * This will add the component and driver to the list of installed components
   * and send the install signal to the computer.
   */
  def add(block: Block, x: Int, y: Int, z: Int, id: Int): Option[IBlockDriver]

  /**
   * Tries to remove the component with the specified ID from the computer.
   *
   * This can fail if there is no such component installed in the computer. The
   * driver's {@see IDriver#close()} function will be called, and the uninstall
   * signal will be sent to the computer.
   */
  def remove(id: Int): Boolean

  // ----------------------------------------------------------------------- //

  /** Starts asynchronous execution of this computer if it isn't running. */
  def start(): Boolean

  /** Stops a computer, possibly asynchronously, possibly blocking. */
  def stop(): Unit

  /**
   * Passively drives the computer and performs driver calls. If this is not
   * called regularly the computer will pause. If a computer is currently
   * trying to perform a driver call, this will perform that driver call in a
   * synchronized manner.
   */
  def update()

  // ----------------------------------------------------------------------- //

  def readFromNBT(nbt: NBTTagCompound)

  def writeToNBT(nbt: NBTTagCompound)
}