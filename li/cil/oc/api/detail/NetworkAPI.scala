package li.cil.oc.api.detail

import net.minecraft.world.IBlockAccess

/** Avoids reflection structural types would induce. */
trait NetworkAPI {
  def joinOrCreateNetwork(world: IBlockAccess, x: Int, y: Int, z: Int)
}