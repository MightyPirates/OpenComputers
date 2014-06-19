package li.cil.oc.common

import com.google.common.cache.CacheBuilder
import li.cil.oc.api.network.ManagedEnvironment

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
}
