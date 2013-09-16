package li.cil.oc.server.computer

import net.minecraft.world.World

/**
 * This has to be implemented by owners of computer instances and allows the
 * computers to access information about the world they live in.
 */
trait IComputerEnvironment {
  def world: World

  /**
   * Get the driver for the component with the specified ID.
   */
  def driver(id: Int): Option[Driver]

  /**
   * Get the component with the specified ID.
   *
   * IDs are assigned by calling the computer's add() function, and mus be
   * tracked by the computer's owner object.
   */
  def component(id: Int): Option[Any]

  /**
   * Called when the computer state changed, so it should be saved again.
   *
   * This is called asynchronously from the Computer's executor thread, so the
   * computer's owner must make sure to handle this in a synchronized fashion.
   */
  def markAsChanged(): Unit
}