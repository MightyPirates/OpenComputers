package li.cil.oc.server.driver

import li.cil.oc.api.driver
import li.cil.oc.{Config, Blocks}
import net.minecraft.world.World

object Keyboard extends driver.Block {
  override def api = Option(getClass.getResourceAsStream(Config.driverPath + "keyboard.lua"))

  override def worksWith(world: World, x: Int, y: Int, z: Int) =
    world.getBlockId(x, y, z) == Blocks.keyboard.blockId
}
