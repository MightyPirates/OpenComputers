package li.cil.oc.util

import li.cil.oc.api.network.Component
import li.cil.oc.api.network.Node
import li.cil.oc.server.component.UpgradeDatabase

import scala.collection.JavaConversions

object DatabaseAccess {
  def databases(node: Node): Iterable[UpgradeDatabase] =
    JavaConversions.iterableAsScalaIterable(node.network.nodes).collect {
      case component: Component => component.host match {
        case db: UpgradeDatabase => db
        case _ => null
      }
    }.filter( _ != null )

  def database(node: Node, address: String): UpgradeDatabase = {
    node.network.node(address) match {
      case component: Component => component.host match {
        case db: UpgradeDatabase => db
        case _ => throw new IllegalArgumentException("not a database")
      }
      case _ => throw new IllegalArgumentException("no such component")
    }
  }

  def withDatabase(node: Node, address: String, f: UpgradeDatabase => Array[AnyRef]): Array[AnyRef] = f(DatabaseAccess.database(node, address))
}
