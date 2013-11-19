package li.cil.oc.util.mods

import java.lang.reflect.InvocationTargetException
import scala.language.existentials

object RedstoneInMotion {
  private val (controller, setup, move, directions) = try {
    val controller = Class.forName("JAKJ.RedstoneInMotion.CarriageControllerEntity")
    val methods = controller.getDeclaredMethods
    val setup = methods.find(_.getName == "SetupMotion").get
    val move = methods.find(_.getName == "Move").get
    val directions = Class.forName("JAKJ.RedstoneInMotion.Directions").getEnumConstants
    (Option(controller), setup, move, directions)
  } catch {
    case _: Throwable => (None, null, null, null)
  }

  def available = controller.isDefined

  def isCarriageController(value: AnyRef) = controller match {
    case Some(clazz) => clazz.isAssignableFrom(value.getClass)
    case _ => false
  }

  def move(controller: AnyRef, direction: Int, simulating: Boolean, anchored: Boolean) {
    if (!isCarriageController(controller))
      throw new IllegalArgumentException("Not a carriage controller.")

    try {
      setup.invoke(controller, directions(direction), Boolean.box(simulating), Boolean.box(anchored))
      move.invoke(controller)
    } catch {
      case e: InvocationTargetException => throw e.getCause
    }
  }
}
