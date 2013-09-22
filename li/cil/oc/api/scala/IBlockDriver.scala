package li.cil.oc.api.scala

import li.cil.oc.api.{ IBlockDriver => IJavaBlockDriver }

import net.minecraft.world.World

trait IBlockDriver extends IJavaBlockDriver with IDriver {
  def component(world: World, x: Int, y: Int, z: Int): Option[AnyRef]

  // ----------------------------------------------------------------------- //

  def getComponent(world: World, x: Int, y: Int, z: Int): Object =
    component(world, x, y, z).orNull
}