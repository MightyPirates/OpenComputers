package li.cil.oc.server.component.machine

import net.minecraft.nbt.NBTTagCompound
import li.cil.oc.server.component.machine.Machine

/**
 * This trait abstracts away any language specific details for the Machine.
 *
 * At some point in the future this may allow us to introduce other languages,
 * e.g. computers that run assembly or non-persistent computers that use a pure
 * Java implementation of Lua.
 */
trait Architecture {
  /**
   * Used to check if the machine is fully initialized. If this is false no
   * signals for detected components will be generated. Avoids duplicate signals
   * if component_added signals are generated in the language's startup script,
   * for already present components (see Lua's init.lua script).
   *
   * @return whether the machine is fully initialized.
   */
  def isInitialized: Boolean

  /**
   * This is called when the amount of memory in the computer may have changed.
   * It is triggered by the tile entity's onInventoryChanged.
   */
  def recomputeMemory()

  /**
   * Performs a synchronized call initialized in a previous call to runThreaded.
   */
  def runSynchronized()

  /**
   * Continues execution of the machine. The first call may be used to
   * initialize the machine (e.g. for Lua we load the libraries in the first
   * call so that the computers boot faster).
   *
   * The resumed state is either Machine.State.SynchronizedReturn, when a
   * synchronized call has been completed (via runSynchronized), or
   * Machine.State.Yielded in all other cases (sleep, interrupt, boot, ...).
   *
   * This is expected to return within a very short time, usually. For example,
   * in Lua this returns as soon as the state yields, and returns at the latest
   * when the Settings.timeout is reached (in which case it forces the state
   * to crash).
   *
   * This is expected to consume a single signal if one is present and return.
   * If returning from a synchronized call this should consume no signal.
   *
   * @param enterState the state that is being resumed.
   * @return the result of the execution. Used to determine the new state.
   */
  def runThreaded(enterState: Machine.State.Value): ExecutionResult

  /**
   * Called when a computer starts up. Used to (re-)initialize the underlying
   * architecture logic. For example, for Lua the creates a new Lua state.
   *
   * This also sets up any built-in APIs for the underlying language, such as
   * querying available memory, listing and interacting with components and so
   * on. If this returns false the computer fails to start.
   *
   * @return whether the architecture was initialized successfully.
   */
  def init(): Boolean

  /**
   * Called when a computer stopped. Used to clean up any handles, memory and
   * so on. For example, for Lua this destroys the Lua state.
   */
  def close()

  /**
   * Restores the state of this architecture as previously saved in save().
   *
   * @param nbt the tag compound to save to.
   */
  def load(nbt: NBTTagCompound)

  /**
   * Saves the architecture for later restoration, e.g. across games or chunk
   * unloads. Used to persist a computer's executions state. For native Lua this
   * uses the Eris library to persist the main coroutine.
   *
   * Note that the tag compound is shared with the Machine, so collisions have
   * to be avoided (see Machine.save for used keys).
   *
   * @param nbt the tag compound to save to.
   */
  def save(nbt: NBTTagCompound)
}
