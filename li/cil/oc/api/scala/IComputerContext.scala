package li.cil.oc.api.scala;

import li.cil.oc.api.{ IComputerContext => IJavaComputerContext }

import net.minecraft.world.World

trait IComputerContext extends IJavaComputerContext {
  def world: World

  def signal(name: String, args: Any*): Boolean

  // ----------------------------------------------------------------------- //

  def getWorld = world

  def signal(name: String, args: Array[Object]): Boolean =
    signal(name, args: _*)
}