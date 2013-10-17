package li.cil.oc.api.detail

import net.minecraft.world.World

/** Avoids reflection structural types would induce. */
trait NetworkAPI {
  def joinOrCreateNetwork(world: World, x: Int, y: Int, z: Int)
}