package li.cil.oc.server

import li.cil.oc.common
import net.minecraft.world.World

object ComponentTracker extends common.ComponentTracker {
  override protected def clear(world: World) = if (!world.isClientSide) super.clear(world)
}
