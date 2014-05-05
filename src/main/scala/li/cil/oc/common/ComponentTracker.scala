package li.cil.oc.common

import li.cil.oc.api.network.ManagedEnvironment
import scala.collection.mutable

/**
 * Keeps track of loaded components by ID. Used to send messages between
 * component representation on server and client without knowledge of their
 * containers. For now this is only used for screens / text buffer components.
 */
abstract class ComponentTracker {
  private val components = mutable.WeakHashMap.empty[String, ManagedEnvironment]

  def add(address: String, component: ManagedEnvironment) {
    components += address -> component
  }

  def remove(address: String) {
    components -= address
  }

  def get(address: String): Option[ManagedEnvironment] = components.get(address)
}
