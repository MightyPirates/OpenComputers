package li.cil.oc.server.component.machine

/**
 * Used by the Machine to determine the result of a call to runThreaded.
 *
 * Do not implement this interface, only use the predefined classes below.
 */
trait ExecutionResult

object ExecutionResult {

  /**
   * Indicates the machine may sleep for the specified number of ticks. This is
   * merely considered a suggestion. If signals are in the queue or are pushed
   * to the queue while sleeping, the sleep will be interrupted and runThreaded
   * will be called so that the next signal is pushed.
   *
   * @param ticks the number of ticks to sleep.
   */
  class Sleep(val ticks: Int) extends ExecutionResult

  /**
   * Indicates tha the computer should shutdown or reboot.
   *
   * @param reboot whether to reboot. If false the computer will stop.
   */
  class Shutdown(val reboot: Boolean) extends ExecutionResult

  /**
   * Indicates that a synchronized call should be performed. The architecture
   * is expected to be in a state that allows the next call to be to
   * runSynchronized instead of runThreaded. This is used to perform calls from
   * the server's main thread, to avoid threading issues when interacting with
   * other objects in the world.
   */
  class SynchronizedCall extends ExecutionResult

  /**
   * Indicates that an error occurred and the computer should crash.
   *
   * @param message the error message.
   */
  class Error(val message: String) extends ExecutionResult

}