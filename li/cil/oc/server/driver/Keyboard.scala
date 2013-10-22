package li.cil.oc.server.driver

import li.cil.oc.Config
import li.cil.oc.api.driver
import net.minecraft.world.World

object Keyboard extends driver.Block {
  override def api = Option(getClass.getResourceAsStream(Config.driverPath + "keyboard.lua"))

  override def worksWith(world: World, x: Int, y: Int, z: Int) = false

  def node(world: World, x: Int, y: Int, z: Int) = throw new IllegalStateException()
}
