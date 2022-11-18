package li.cil.oc.common

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import li.cil.oc.api.network.ManagedEnvironment
import net.minecraft.util.RegistryKey
import net.minecraft.world.World
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

import scala.collection.JavaConverters.asJavaIterable
import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.mutable

/**
 * Keeps track of loaded components by ID. Used to send messages between
 * component representation on server and client without knowledge of their
 * containers. For now this is only used for screens / text buffer components.
 */
abstract class ComponentTracker {
  private val worlds = mutable.Map.empty[RegistryKey[World], Cache[String, ManagedEnvironment]]

  private def components(world: World) = {
    worlds.getOrElseUpdate(world.dimension,
      com.google.common.cache.CacheBuilder.newBuilder().
        weakValues().
        asInstanceOf[CacheBuilder[String, ManagedEnvironment]].
        build[String, ManagedEnvironment]())
  }

  def add(world: World, address: String, component: ManagedEnvironment) {
    this.synchronized {
      components(world).put(address, component)
    }
  }

  def remove(world: World, component: ManagedEnvironment) {
    this.synchronized {
      components(world).invalidateAll(asJavaIterable(components(world).asMap().filter(_._2 == component).keys))
      components(world).cleanUp()
    }
  }

  def get(world: World, address: String): Option[ManagedEnvironment] = this.synchronized {
    components(world).cleanUp()
    Option(components(world).getIfPresent(address))
  }

  @SubscribeEvent
  def onWorldUnload(e: WorldEvent.Unload): Unit = e.getWorld match {
    case world: World => clear(world)
    case _ =>
  }

  protected def clear(world: World): Unit = this.synchronized {
    components(world).invalidateAll()
    components(world).cleanUp()
  }
}
