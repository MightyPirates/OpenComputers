package li.cil.oc.server.driver

import java.util
import java.util.logging.Level
import li.cil.oc.api.driver.Converter
import li.cil.oc.{OpenComputers, api}
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable.ArrayBuffer

/**
 * This class keeps track of registered drivers and provides installation logic
 * for each registered driver.
 *
 * Each component type must register its driver with this class to be used with
 * computers, since this class is used to determine whether an object is a
 * valid component or not.
 *
 * All drivers must be installed once the game starts - in the init phase - and
 * are then injected into all computers started up past that point. A driver is
 * a set of functions made available to the computer. These functions will
 * usually require a component of the type the driver wraps to be installed in
 * the computer, but may also provide context-free functions.
 */
private[oc] object Registry extends api.detail.DriverAPI {
  val blocks = ArrayBuffer.empty[api.driver.Block]

  val items = ArrayBuffer.empty[api.driver.Item]

  val converters = ArrayBuffer.empty[api.driver.Converter]

  /** Used to keep track of whether we're past the init phase. */
  var locked = false

  override def add(driver: api.driver.Block) {
    if (locked) throw new IllegalStateException("Please register all drivers in the init phase.")
    if (!blocks.contains(driver)) blocks += driver
  }

  override def add(driver: api.driver.Item) {
    if (locked) throw new IllegalStateException("Please register all drivers in the init phase.")
    if (!blocks.contains(driver)) items += driver
  }

  override def add(converter: Converter) {
    if (locked) throw new IllegalStateException("Please register all converters in the init phase.")
    if (!converters.contains(converter)) converters += converter
  }

  // TODO Move this into the API?
  def blockDriverFor(world: World, x: Int, y: Int, z: Int) =
    blocks.filter(_.worksWith(world, x, y, z)) match {
      case drivers if !drivers.isEmpty => Some(new CompoundBlockDriver(drivers: _*))
      case _ => None
    }

  // TODO Move this into the API?
  def itemDriverFor(stack: ItemStack) =
    if (stack != null) items.find(_.worksWith(stack)) match {
      case None => None
      case Some(driver) => Some(driver)
    }
    else None

  def convert(value: Array[AnyRef]) = if (value != null) value.map(convertRecursively) else null

  def convertRecursively(value: AnyRef): AnyRef = value match {
    case null | Unit | None => null
    case arg: java.lang.Boolean => arg
    case arg: java.lang.Byte => arg
    case arg: java.lang.Character => arg
    case arg: java.lang.Short => arg
    case arg: java.lang.Integer => arg
    case arg: java.lang.Long => arg
    case arg: java.lang.Float => arg
    case arg: java.lang.Double => arg
    case arg: java.lang.String => arg

    case arg: Array[Boolean] => arg
    case arg: Array[Byte] => arg
    case arg: Array[Character] => arg
    case arg: Array[Short] => arg
    case arg: Array[Integer] => arg
    case arg: Array[Long] => arg
    case arg: Array[Float] => arg
    case arg: Array[Double] => arg
    case arg: Array[String] => arg

    case arg: Array[_] => arg.map {
      case (value: AnyRef) => convertRecursively(value)
    }
    case arg: Map[_, _] => arg.map {
      case (key: AnyRef, null) =>convertRecursively(key) -> null
      case (key: AnyRef, value: AnyRef) => convertRecursively(key) -> convertRecursively(value)
    }
    case arg: java.util.Map[_, _] => arg.map {
      case (key: AnyRef, null) =>convertRecursively(key) -> null
      case (key: AnyRef, value: AnyRef) => convertRecursively(key) -> convertRecursively(value)
    }

    case arg =>
      val result = new util.HashMap[AnyRef, AnyRef]()
      converters.foreach(converter => try converter.convert(arg, result) catch {
        case t: Throwable => OpenComputers.log.log(Level.WARNING, "Type converter threw an exception.", t)
      })
      if (result.isEmpty) null
      else result
  }
}
