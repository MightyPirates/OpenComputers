package li.cil.oc.server.drivers

import li.cil.oc.Blocks
import li.cil.oc.api.IBlockDriver
import net.minecraft.world.World

object KeyboardDriver extends IBlockDriver {
  override def api = Option(getClass.getResourceAsStream("/assets/opencomputers/lua/keyboard.lua"))

  override def worksWith(world: World, x: Int, y: Int, z: Int) =
    world.getBlockId(x, y, z) == Blocks.keyboard.blockId
}
