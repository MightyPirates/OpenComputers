package li.cil.oc.common.computer

import li.cil.oc.server.components.IComponent
import li.cil.oc.server.computer.Driver
import net.minecraft.nbt.NBTTagCompound

/**
 * This interface is used to be able to use the same basic type for storing a
 * computer on both client and server. There are two implementations of this,
 * one for the server, which does hold the actual computer logic, and one for
 * the client, which does nothing at all.
 */
trait IComputer {
  /**
   * Tries to add the specified component to the computer.
   *
   * This can fail if another component with that ID is already installed in
   * the computer. This will add the component and driver to the list of
   * installed components and send the install signal to the computer, as well
   * as call the install function of the driver.
   *
   * @param component the component object.
   * @param driver the driver used for the component.
   * @return true if the component was installed, fals otherwise.
   */
  def add(component: Any, driver: Driver): Boolean

  /**
   * Tries to remove the component with the specified ID from the computer.
   *
   * This can fail if there is no such component installed in the computer. The
   * driver's uninstall function will be called, and the uninstall signal will
   * be sent to the computer.
   *
   * @param id the id of the component to remove.
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