package li.cil.oc.common.component

import net.minecraft.nbt.NBTTagCompound

/**
 * This interface is used to be able to use the same basic type for storing a
 * computer on both client and server. There are two implementations of this,
 * one for the server, which does hold the actual computer logic, and one for
 * the client, which does nothing at all.
 */
trait Computer {
  /** Starts asynchronous execution of this computer. */
  def start(): Boolean

  /** Stops a computer, possibly asynchronously. */
  def stop(): Boolean

  def isRunning: Boolean

  /**
   * Passively drives the computer and performs synchronized calls. If this is
   * not called regularly the computer will pause. If a computer is currently
   * trying to perform a synchronized call, this will perform that call.
   */
  def update()

  def signal(name: String, args: Any*): Boolean

  def recomputeMemory()

  // ----------------------------------------------------------------------- //

  def load(nbt: NBTTagCompound)

  def save(nbt: NBTTagCompound)
}