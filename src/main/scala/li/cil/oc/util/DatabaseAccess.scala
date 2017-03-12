package li.cil.oc.util

import li.cil.oc.api.network.{Component, ComponentNode, Node, NodeComponent}
import li.cil.oc.server.component.UpgradeDatabase

object DatabaseAccess {
  def withDatabase(node: Node, address: String, f: UpgradeDatabase => Array[AnyRef]): Array[AnyRef] = {
    node.getNetwork.node(address) match {
      case component: ComponentNode => component.getContainer match {
        case database: UpgradeDatabase => f(database)
        case _ => throw new IllegalArgumentException("not a database")
      }
      case _ => throw new IllegalArgumentException("no such component")
    }
  }
}
