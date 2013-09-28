package li.cil.oc.server.computer

import li.cil.oc.api.network.{Visibility, Node}
import net.minecraft.world.World

/**
 * This has to be implemented by owners of computer instances and allows the
 * computers to access information about the world they live in.
 */
trait Environment extends Node {
  override def name = "computer"

  override def visibility = Visibility.Network

  def world: World

  /**
   * Called when the computer state changed, so it should be saved again.
   *
   * This is called asynchronously from the Computer's executor thread, so the
   * computer's owner must make sure to handle this in a synchronized fashion.
   */
  def markAsChanged(): Unit
}