package li.cil.oc.common

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import li.cil.oc.api.network.{EnvironmentItem, ManagedEnvironment}
import net.minecraft.world.World
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

/**
 * Keeps track of loaded components by ID. Used to send messages between
 * component representation on server and client without knowledge of their
 * containers. For now this is only used for screens / text buffer components.
 */
abstract class ComponentTracker {
  private val worlds = mutable.Map.empty[Int, Cache[String, EnvironmentItem]]

  private def components(world: World) = {
    worlds.getOrElseUpdate(world.provider.getDimension,
      com.google.common.cache.CacheBuilder.newBuilder().
        weakValues().
        asInstanceOf[CacheBuilder[String, EnvironmentItem]].
        build[String, EnvironmentItem]())
  }

  def add(world: World, address: String, component: EnvironmentItem) {
    this.synchronized {
      components(world).put(address, component)
    }
  }

  def remove(world: World, component: EnvironmentItem) {
    this.synchronized {
      components(world).invalidateAll(asJavaIterable(components(world).asMap().filter(_._2 == component).keys))
      components(world).cleanUp()
    }
  }

  def get(world: World, address: String): Option[EnvironmentItem] = this.synchronized {
    components(world).cleanUp()
    Option(components(world).getIfPresent(address))
  }

  @SubscribeEvent
  def onWorldUnload(e: WorldEvent.Unload): Unit = clear(e.getWorld)

  protected def clear(world: World): Unit = this.synchronized {
    components(world).invalidateAll()
    components(world).cleanUp()
  }
}
