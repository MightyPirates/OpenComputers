import li.cil.oc.api
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Visibility
import li.cil.oc.server.network.Network
import li.cil.oc.server.network.{Node => MutableNode}
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar

import scala.collection.convert.WrapAsScala._

@RunWith(classOf[JUnitRunner])
class NetworkTest extends FlatSpec with MockitoSugar {
  Network.isServer = () => true
  api.API.network = Network

  val host = mock[Environment]

  "A Node" should "not belong to a network after creation" in {
    val node = api.Network.newNode(host, Visibility.Network).create()
    assert(node.network == null)
  }

  it should "belong to a network after joining a new network" in {
    val node = api.Network.newNode(host, Visibility.Network).create()
    api.Network.joinNewNetwork(node)
    assert(node.network != null)
  }

  it should "not belong to a network after being removed from its new network" in {
    val node = api.Network.newNode(host, Visibility.Network).create()
    api.Network.joinNewNetwork(node)
    node.remove()
    assert(node.network == null)
  }

  it should "have a neighbor after being connected to another node" in {
    val node1 = api.Network.newNode(host, Visibility.Network).create()
    api.Network.joinNewNetwork(node1)
    val node2 = api.Network.newNode(host, Visibility.Network).create()
    node1.connect(node2)
    assert(node1.neighbors.nonEmpty)
    assert(node2.neighbors.nonEmpty)
    assert(node1.isNeighborOf(node2))
    assert(node2.isNeighborOf(node1))
  }

  it should "be reachable by neighbors when visibility is set to Neighbors" in {
    val node1 = api.Network.newNode(host, Visibility.Neighbors).create()
    api.Network.joinNewNetwork(node1)
    val node2 = api.Network.newNode(host, Visibility.Network).create()
    node1.connect(node2)
    assert(node1.canBeReachedFrom(node2))
  }

  it should "be in the same network as nodes it is connected to" in {
    val node1 = api.Network.newNode(host, Visibility.Network).create()
    api.Network.joinNewNetwork(node1)
    val node2 = api.Network.newNode(host, Visibility.Network).create()
    node1.connect(node2)
    val node3 = api.Network.newNode(host, Visibility.Network).create()
    node2.connect(node3)

    assert(node1.network == node2.network)
    assert(node2.network == node3.network)
    assert(node1.network == node3.network)
  }

  it should "have a different address than nodes it is connected to" in {
    val node1 = api.Network.newNode(host, Visibility.Network).create()
    api.Network.joinNewNetwork(node1)
    val node2 = api.Network.newNode(host, Visibility.Network).create()
    node1.connect(node2)
    val node3 = api.Network.newNode(host, Visibility.Network).create()
    node2.connect(node3)

    assert(node1.address != node2.address)
    assert(node2.address != node3.address)
    assert(node1.address != node3.address)
  }

  it should "not be reachable by non neighbors when visibility is set to Neighbors" in {
    val node1 = api.Network.newNode(host, Visibility.Neighbors).create()
    api.Network.joinNewNetwork(node1)
    val node2 = api.Network.newNode(host, Visibility.Network).create()
    node1.connect(node2)
    val node3 = api.Network.newNode(host, Visibility.Network).create()
    node2.connect(node3)
    assert(!node1.canBeReachedFrom(node3))
  }

  it should "be reachable by all nodes when visibility is set to Network" in {
    val node1 = api.Network.newNode(host, Visibility.Network).create()
    api.Network.joinNewNetwork(node1)
    val node2 = api.Network.newNode(host, Visibility.Network).create()
    node1.connect(node2)
    val node3 = api.Network.newNode(host, Visibility.Network).create()
    node2.connect(node3)
    assert(node1.canBeReachedFrom(node2))
    assert(node1.canBeReachedFrom(node3))
  }

  it should "not be reachable by any node when visibility is set to None" in {
    val node1 = api.Network.newNode(host, Visibility.None).create()
    api.Network.joinNewNetwork(node1)
    val node2 = api.Network.newNode(host, Visibility.Network).create()
    node1.connect(node2)
    val node3 = api.Network.newNode(host, Visibility.Network).create()
    node2.connect(node3)
    assert(!node1.canBeReachedFrom(node2))
    assert(!node1.canBeReachedFrom(node3))
  }

  it should "be in a separate network after a netsplit" in {
    val node1 = api.Network.newNode(host, Visibility.Network).create()
    api.Network.joinNewNetwork(node1)
    val node2 = api.Network.newNode(host, Visibility.Network).create()
    node1.connect(node2)
    val node3 = api.Network.newNode(host, Visibility.Network).create()
    node2.connect(node3)

    node2.remove()

    assert(node1.network != null)
    assert(node2.network == null)
    assert(node3.network != null)
    assert(node1.network != node3.network)
  }

  it should "change its address when joining a network containing a node with its address" in {
    val node1 = api.Network.newNode(host, Visibility.Network).create()
    api.Network.joinNewNetwork(node1)
    val node2 = api.Network.newNode(host, Visibility.Network).create()
    node2.asInstanceOf[MutableNode].address = node1.address
    node1.connect(node2)
    assert(node1.address != node2.address)
  }

  "A Network" should "keep its local layout after being merged with another network" in {
    val node1 = api.Network.newNode(host, Visibility.Network).create()
    api.Network.joinNewNetwork(node1)
    val node2 = api.Network.newNode(host, Visibility.Network).create()
    node1.connect(node2)
    val node3 = api.Network.newNode(host, Visibility.Network).create()
    node2.connect(node3)

    val node4 = api.Network.newNode(host, Visibility.Network).create()
    api.Network.joinNewNetwork(node4)
    val node5 = api.Network.newNode(host, Visibility.Network).create()
    node4.connect(node5)
    val node6 = api.Network.newNode(host, Visibility.Network).create()
    node5.connect(node6)

    node2.connect(node5)

    assert(node1.neighbors.size == 1 && node1.isNeighborOf(node2))
    assert(node3.neighbors.size == 1 && node3.isNeighborOf(node2))

    assert(node4.neighbors.size == 1 && node4.isNeighborOf(node5))
    assert(node6.neighbors.size == 1 && node6.isNeighborOf(node5))

    assert(node2.isNeighborOf(node5))
  }

  it should "keep its local layout after being merged with another network containing nodes with duplicate addresses at bridge points" in {
    val node1 = api.Network.newNode(host, Visibility.Network).create()
    api.Network.joinNewNetwork(node1)
    val node2 = api.Network.newNode(host, Visibility.Network).create()
    node1.connect(node2)
    val node3 = api.Network.newNode(host, Visibility.Network).create()
    node1.connect(node3)
    val node4 = api.Network.newNode(host, Visibility.Network).create()
    node3.connect(node4)

    val node5 = api.Network.newNode(host, Visibility.Network).create()
    node5.asInstanceOf[MutableNode].address = node1.address
    api.Network.joinNewNetwork(node5)
    val node6 = api.Network.newNode(host, Visibility.Network).create()
    node6.asInstanceOf[MutableNode].address = node2.address
    node5.connect(node6)
    val node7 = api.Network.newNode(host, Visibility.Network).create()
    node7.asInstanceOf[MutableNode].address = node3.address
    node5.connect(node7)
    val node8 = api.Network.newNode(host, Visibility.Network).create()
    node8.asInstanceOf[MutableNode].address = node4.address
    node7.connect(node8)

    node3.connect(node7)

    assert(node1.neighbors.size == 2 && node1.isNeighborOf(node2) && node1.isNeighborOf(node3))
    assert(node2.neighbors.size == 1 && node2.isNeighborOf(node1))
    assert(node3.neighbors.size == 3 && node3.isNeighborOf(node1) && node3.isNeighborOf(node4) && node3.isNeighborOf(node7))
    assert(node4.neighbors.size == 1 && node4.isNeighborOf(node3))

    assert(node5.neighbors.size == 2 && node5.isNeighborOf(node6) && node5.isNeighborOf(node7))
    assert(node6.neighbors.size == 1 && node6.isNeighborOf(node5))
    assert(node7.neighbors.size == 3 && node7.isNeighborOf(node5) && node7.isNeighborOf(node8) && node7.isNeighborOf(node3))
    assert(node8.neighbors.size == 1 && node8.isNeighborOf(node7))
  }

  it should "not error when nodes disconnect themselves in a remapping operation" in {
    val host = new Environment {
      val node1 = api.Network.newNode(this, Visibility.Network).create()
      val node2 = api.Network.newNode(this, Visibility.Network).create()

      api.Network.joinNewNetwork(node1)

      override def node: Node = node1

      override def onMessage(message: Message): Unit = {}

      override def onConnect(node: Node): Unit = {
        if (node == node1) {
          node.connect(node2)
        }
      }

      override def onDisconnect(node: Node): Unit = {
        if (node == node1) {
          node2.remove()
        }
      }
    }

    val node3 = api.Network.newNode(host, Visibility.Network).create()
    node3.asInstanceOf[MutableNode].address = host.node.address
    api.Network.joinNewNetwork(node3)

    node3.connect(host.node)

    assert(host.node1.neighbors.size == 2 && host.node1.isNeighborOf(host.node2) && host.node1.isNeighborOf(node3))
    assert(host.node2.neighbors.size == 1 && host.node2.isNeighborOf(host.node1))
    assert(node3.neighbors.size == 1 && node3.isNeighborOf(host.node1))
  }
}
