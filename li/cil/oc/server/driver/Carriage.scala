package li.cil.oc.server.driver

import li.cil.oc.api.driver
import li.cil.oc.server.component
import net.minecraft.world.World

object Carriage extends driver.Block {
  private val (carriageControllerClass) = try {
    Class.forName("JAKJ.RedstoneInMotion.CarriageControllerEntity")
  } catch {
    case _: Throwable => null
  }

  def worksWith(world: World, x: Int, y: Int, z: Int) =
    Option(world.getBlockTileEntity(x, y, z)) match {
      case Some(entity) if checkClass(entity) => true
      case _ => false
    }

  def createEnvironment(world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case controller if checkClass(controller) =>
        new component.Carriage(controller)
      case _ => null
    }

  private def checkClass(value: Object) =
    carriageControllerClass != null && carriageControllerClass.isAssignableFrom(value.getClass)
}
