package li.cil.oc.server.driver

import java.util

import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.api.driver.Converter
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.InventoryProvider
import li.cil.oc.api.driver.item.HostAware
import li.cil.oc.api.machine.Value
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network.ManagedEnvironment
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.math.ScalaNumber

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
  val blocks = mutable.ArrayBuffer.empty[api.driver.Block]

  val sidedBlocks = mutable.ArrayBuffer.empty[api.driver.SidedBlock]

  val items = mutable.ArrayBuffer.empty[api.driver.Item]

  val converters = mutable.ArrayBuffer.empty[api.driver.Converter]

  val environmentProviders = mutable.ArrayBuffer.empty[api.driver.EnvironmentProvider]

  val inventoryProviders = mutable.ArrayBuffer.empty[api.driver.InventoryProvider]

  val blacklist = mutable.ArrayBuffer.empty[(ItemStack, mutable.Set[Class[_]])]

  /** Used to keep track of whether we're past the init phase. */
  var locked = false

  override def add(driver: api.driver.Block) {
    if (locked) throw new IllegalStateException("Please register all drivers in the init phase.")
    if (!blocks.contains(driver)) {
      OpenComputers.log.debug(s"Registering block driver ${driver.getClass.getName}.")
      blocks += driver
    }
  }

  override def add(driver: api.driver.SidedBlock) {
    if (locked) throw new IllegalStateException("Please register all drivers in the init phase.")
    if (!sidedBlocks.contains(driver)) {
      OpenComputers.log.debug(s"Registering block driver ${driver.getClass.getName}.")
      sidedBlocks += driver
    }
  }

  override def add(driver: api.driver.Item) {
    if (locked) throw new IllegalStateException("Please register all drivers in the init phase.")
    if (!items.contains(driver)) {
      OpenComputers.log.debug(s"Registering item driver ${driver.getClass.getName}.")
      items += driver
    }
  }

  override def add(converter: Converter) {
    if (locked) throw new IllegalStateException("Please register all converters in the init phase.")
    if (!converters.contains(converter)) {
      OpenComputers.log.debug(s"Registering converter ${converter.getClass.getName}.")
      converters += converter
    }
  }

  override def add(provider: EnvironmentProvider): Unit = {
    if (locked) throw new IllegalStateException("Please register all environment providers in the init phase.")
    if (!environmentProviders.contains(provider)) {
      OpenComputers.log.debug(s"Registering environment provider ${provider.getClass.getName}.")
      environmentProviders += provider
    }
  }

  override def add(provider: InventoryProvider): Unit = {
    if (locked) throw new IllegalStateException("Please register all inventory providers in the init phase.")
    if (!inventoryProviders.contains(provider)) {
      OpenComputers.log.debug(s"Registering inventory provider ${provider.getClass.getName}.")
      inventoryProviders += provider
    }
  }

  // TODO Remove in OC 1.7
  override def driverFor(world: World, pos: BlockPos) = {
    driverFor(world, pos, null) match {
      case driver: api.driver.SidedBlock => new api.driver.Block {
        override def worksWith(world: World, pos: BlockPos): Boolean = driver.worksWith(world, pos, null)

        override def createEnvironment(world: World, pos: BlockPos): ManagedEnvironment = driver.createEnvironment(world, pos, null)
      }
      case _ => null
    }
  }

  override def driverFor(world: World, pos: BlockPos, side: EnumFacing): api.driver.SidedBlock =
    (sidedBlocks.filter(_.worksWith(world, pos, side)), blocks.filter(_.worksWith(world, pos))) match {
      case (sidedDrivers, drivers) if sidedDrivers.nonEmpty || drivers.nonEmpty => new CompoundBlockDriver(sidedDrivers.toArray, drivers.toArray)
      case _ => null
    }

  override def driverFor(stack: ItemStack, host: Class[_ <: EnvironmentHost]) =
    if (stack != null) {
      val hostAware = items.collect {
        case driver: HostAware if driver.worksWith(stack) => driver
      }
      if (hostAware.nonEmpty) {
        hostAware.find(_.worksWith(stack, host)).orNull
      }
      else driverFor(stack)
    }
    else null

  override def driverFor(stack: ItemStack) =
    if (stack != null) items.find(_.worksWith(stack)).orNull
    else null

  override def environmentFor(stack: ItemStack): Class[_] = {
    environmentProviders.map(provider => provider.getEnvironment(stack)).collectFirst {
      case clazz: Class[_] => clazz
    }.orNull
  }

  override def inventoryFor(stack: ItemStack, player: EntityPlayer): IInventory = {
    inventoryProviders.find(provider => provider.worksWith(stack, player)).
      map(provider => provider.getInventory(stack, player)).
      orNull
  }

  override def blockDrivers = blocks.toSeq

  override def itemDrivers = items.toSeq

  def blacklistHost(stack: ItemStack, host: Class[_]) {
    blacklist.find(_._1.isItemEqual(stack)) match {
      case Some((_, hosts)) => hosts += host
      case _ => blacklist.append((stack, mutable.Set(host)))
    }
  }

  def convert(value: Array[AnyRef]) = if (value != null) value.map(arg => convertRecursively(arg, new util.IdentityHashMap())) else null

  def convertRecursively(value: Any, memo: util.IdentityHashMap[AnyRef, AnyRef], force: Boolean = false): AnyRef = {
    val valueRef = value match {
      case number: ScalaNumber => number.underlying
      case reference: AnyRef => reference
      case null => null
      case primitive => primitive.asInstanceOf[AnyRef]
    }
    if (!force && memo.containsKey(valueRef)) {
      memo.get(valueRef)
    }
    else valueRef match {
      case null | Unit | None => null

      case arg: java.lang.Boolean => arg
      case arg: java.lang.Byte => arg
      case arg: java.lang.Character => arg
      case arg: java.lang.Short => arg
      case arg: java.lang.Integer => arg
      case arg: java.lang.Long => arg
      case arg: java.lang.Float => arg
      case arg: java.lang.Double => arg
      case arg: java.lang.Number => Double.box(arg.doubleValue())
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

      case arg: Value => arg

      case arg: Array[_] => convertList(arg, arg.zipWithIndex.iterator, memo)
      case arg: Product => convertList(arg, arg.productIterator.zipWithIndex, memo)
      case arg: Seq[_] => convertList(arg, arg.zipWithIndex.iterator, memo)

      case arg: Map[_, _] => convertMap(arg, arg, memo)
      case arg: mutable.Map[_, _] => convertMap(arg, arg.toMap, memo)
      case arg: java.util.Map[_, _] => convertMap(arg, arg.toMap, memo)

      case arg: Iterable[_] => convertList(arg, arg.zipWithIndex.toIterator, memo)
      case arg: java.lang.Iterable[_] => convertList(arg, arg.zipWithIndex.iterator, memo)

      case arg =>
        val converted = new util.HashMap[AnyRef, AnyRef]()
        memo += arg -> converted
        converters.foreach(converter => try converter.convert(arg, converted) catch {
          case t: Throwable => OpenComputers.log.warn("Type converter threw an exception.", t)
        })
        if (converted.isEmpty) {
          memo += arg -> arg.toString
          arg.toString
        }
        else {
          // This is a little nasty but necessary because we need to keep the
          // 'converted' value up-to-date for any reference created to it in
          // the following convertRecursively call. For example:
          // - Converter C is called for A with map M.
          // - C puts A into M again.
          // - convertRecursively(M) encounters A in the memoization map, uses M.
          //   That M is then 'wrong', as in not fully converted. Hence the clear
          //   plus copy action afterwards.
          memo += converted -> converted // Makes convertMap re-use the map.
          convertRecursively(converted, memo, force = true)
          memo -= converted
          if (converted.size == 1 && converted.containsKey("oc:flatten")) {
            val value = converted.get("oc:flatten")
            memo += arg -> value // Update memoization map.
            value
          }
          else {
            converted
          }
        }
    }
  }

  def convertList(obj: AnyRef, list: Iterator[(Any, Int)], memo: util.IdentityHashMap[AnyRef, AnyRef]) = {
    val converted = mutable.ArrayBuffer.empty[AnyRef]
    memo += obj -> converted
    for ((value, index) <- list) {
      converted += convertRecursively(value, memo)
    }
    converted.toArray
  }

  def convertMap(obj: AnyRef, map: Map[_, _], memo: util.IdentityHashMap[AnyRef, AnyRef]) = {
    val converted = memo.getOrElseUpdate(obj, mutable.Map.empty[AnyRef, AnyRef]) match {
      case map: mutable.Map[AnyRef, AnyRef]@unchecked => map
      case map: java.util.Map[AnyRef, AnyRef]@unchecked => mapAsScalaMap(map)
    }
    map.collect {
      case (key: AnyRef, value: AnyRef) => converted += convertRecursively(key, memo) -> convertRecursively(value, memo)
    }
    memo.get(obj)
  }
}
