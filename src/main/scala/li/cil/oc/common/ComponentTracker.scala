package li.cil.oc.common

import com.google.common.cache.CacheBuilder
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.api.network.ManagedEnvironment
import net.minecraft.world.World
import net.minecraftforge.event.world.WorldEvent

/**
 * Keeps track of loaded components by ID. Used to send messages between
 * component representation on server and client without knowledge of their
 * containers. For now this is only used for screens / text buffer components.
 */
abstract class ComponentTracker {
  private val components = com.google.common.cache.CacheBuilder.newBuilder().
    weakValues().
    asInstanceOf[CacheBuilder[String, ManagedEnvironment]].
    build[String, ManagedEnvironment]()

  def add(address: String, component: ManagedEnvironment) {
    this.synchronized {
      components.put(address, component)
    }
  }

  def remove(address: String) {
    this.synchronized {
      components.invalidate(address)
      components.cleanUp()
    }
  }

  def get(address: String): Option[ManagedEnvironment] = this.synchronized {
    components.cleanUp()
    Option(components.getIfPresent(address))
  }

  @SubscribeEvent
  def onWorldUnload(e: WorldEvent.Unload): Unit = clear(e.world)

  protected def clear(world: World): Unit = this.synchronized {
    components.invalidateAll()
    components.cleanUp()
  }
}
