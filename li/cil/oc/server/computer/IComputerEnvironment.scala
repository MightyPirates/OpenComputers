package li.cil.oc.server.computer

import net.minecraft.world.World

/**
 * This has to be implemented by owners of computer instances and allows the
 * computers to access information about the world they live in.
 */
trait IComputerEnvironment {
  def world: World

  /** Called when the computer state changed, so it should be saved again. */
  def markAsChanged(): Unit
}