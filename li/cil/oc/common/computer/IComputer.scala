package li.cil.oc.common.computer

import net.minecraft.nbt.NBTTagCompound

/**
 * This interface is used to be able to use the same basic type for storing a
 * computer on both client and server. There are two implementations of this,
 * one for the server, which does hold the actual computer logic, and one for
 * the client, which does nothing at all.
 */
trait IComputer {
  /** Starts asynchronous execution of this computer if it isn't running. */
  def start(): Boolean

  /** Stops a computer, possibly asynchronously, possibly blocking. */
  def stop(): Boolean

  /** Whether the computer is currently running. */
  def isRunning: Boolean

  /**
   * Passively drives the computer and performs driver calls. If this is not
   * called regularly the computer will pause. If a computer is currently
   * trying to perform a driver call, this will perform that driver call in a
   * synchronized manner.
   */
  def update()

  def signal(name: String, args: Any*): Boolean

  // ----------------------------------------------------------------------- //

  def load(nbt: NBTTagCompound)

  def save(nbt: NBTTagCompound)
}