package li.cil.oc.api

import li.cil.oc.api.detail.NetworkAPI
import li.cil.oc.api.network.Node
import net.minecraft.world.IBlockAccess

/**
 * Interface for interacting with component networks.
 * <p/>
 * Computers and components form ad-hoc "networks" when placed next to each
 * other. They allow computers to communicate with the components attached to
 * them, as well as components to send signals to computers they are attached to
 * (and even among each other).
 * <p/>
 * Whenever a networkable component is placed, it should first scan its
 * neighbors to see if a network already exists. If so, it should join that
 * network. If multiple different networks are adjacent it should join one and
 * then merge it with the other(s). If no networks exist, it should create a new
 * one. All this logic is provided by `Network.joinOrCreateNetwork`.
 * <p/>
 * Note that for network nodes implemented in `TileEntities` adding and
 * removal is automatically provided on chunk load and unload. When a block is
 * placed or broken you will have to implement this logic yourself (i.e. call
 * `Network.joinOrCreateNetwork` in `onBlockAdded` and `Network.remove` in
 * `breakBlock`.
 * <p/>
 * All other kinds of nodes have to be managed manually. See `Node`.
 * <p/>
 * There are a couple of system messages to be aware of. These are all sent by
 * the network manager itself:
 * <ul>
 * <li>`network.connect` is generated when a node is added to the network,
 * with the added node as the sender. This will also be sent to the nodes of
 * the other network, when a network merges with another one (both ways).</li>
 * <li>`network.disconnect` is generated when a node is removed from the
 * network, with the removed node as the sender. This will also be sent to the
 * nodes of the other network(s), when a network is split (all pairs).</li>
 * </ul>
 * <p/>
 * IMPORTANT: do *not* implement this interface yourself and create
 * instances of your own network implementation; this will lead to
 * incompatibilities with the built-in network implementation (which can only
 * merge with other networks of its own type). Always use the methods provided
 * in `Network` to create and join networks.
 */
trait Network {
  /**
   * Adds a new node connection in the network.
   * <p/>
   * This is used by nodes to join an existing network. At least one of the two
   * nodes must already be in the network. If one of the nodes is not yet in the
   * network, it will be added to the network. If both nodes are already in the
   * network only the connection between the two nodes is added. If one of the
   * nodes is not in this network but in another network, the networks will be
   * merged.
   * <p/>
   * This way of adding nodes is used to build an internal graph to allow
   * properly splitting networks when nodes are removed.
   *
   * @param nodeA the first node.
   * @param nodeB the second node.
   * @return true if a new connection between the two nodes was added; false if
   *         the connection already existed.
   * @throws IllegalArgumentException if neither node is in this network.
   */
  def connect(nodeA: Node, nodeB: Node): Boolean

  /**
   * Removes a node connection in the network.
   * <p/>
   * Both nodes must be part of this network.
   * <p/>
   * This can be useful for cutting connections that depend on some condition
   * that does not involve the nodes' actual existence in the network, such as
   * the distance between two nodes, for example (think access points of a
   * wireless network).
   *
   * @param nodeA the first node.
   * @param nodeB the second node.
   * @return true if the connection was cut; false if there was none.
   * @throws IllegalArgumentException if the nodes are not in this network.
   */
  def disconnect(nodeA: Node, nodeB: Node): Boolean

  /**
   * Removes a node from the network.
   * <p/>
   * This should be called by nodes when they are destroyed (e.g. `breakBlock`)
   * or unloaded. Removing the node can lead to one or more new networks if it
   * was the a bridge node, i.e. the only node connecting the resulting
   * networks.
   *
   * @param node the node to remove from the network.
   * @return true if the node was removed; false if it wasn't in the network.
   */
  def remove(node: Node): Boolean

  // ----------------------------------------------------------------------- //

  /**
   * Get the network node with the specified address.
   *
   * @param address the address of the node to get.
   * @return the node with that address.
   */
  def node(address: String): Option[Node]

  /**
   * The list of all addressed nodes in this network.
   *
   * @return the list of nodes in this network.
   */
  def nodes: Iterable[Node]

  /**
   * The list of addressed nodes in the network visible to the specified node.
   * <p/>
   * This does *not* include nodes with a visibility of `Visibility.None` or
   * a visibility of `Visibility.Neighbors` when there is no direct connection
   * between that node and the reference node.
   * <p/>
   * This does *not* include the node itself.
   * <p/>
   * This can be useful when performing a delayed initialization of a node.
   * For example, computers will use this when starting up to generate
   * `component_added` signals for all visible nodes in the network.
   *
   * @param reference the node to get the visible other nodes for.
   * @return the nodes visible to the specified node.
   */
  def nodes(reference: Node): Iterable[Node]

  /**
   * The list of nodes the specified node is directly connected to.
   * <p/>
   * This *does* include nodes with a visibility of `Visibility.None`.
   * <p/>
   * This does *not* include the node itself.
   * <p/>
   * This can be used to verify arguments for components that should only work
   * for other components that are directly connected to them, for example.
   *
   * @param node the node to get the neighbors for.
   * @return a list of nodes the node is directly connect to.
   * @throws IllegalArgumentException if the specified node is not in this network.
   */
  def neighbors(node: Node): Iterable[Node]

  // ----------------------------------------------------------------------- //

  /**
   * Sends a message to the node with the specified address.
   * <p/>
   * If the target node with that address has a visibility of `Visibility.None`
   * the message will *not* be delivered to that node. If the target node with
   * that address has a visibility of `Visibility.Neighbors` and the source
   * node is not directly connected to the target the message will *not* be
   * delivered to that node.
   * <p/>
   * Messages should have a unique name to allow differentiating them when
   * handling them in a network node. For example, computers will try to parse
   * messages named `computer.signal` by converting the message data to a
   * signal and inject that signal into the Lua VM, so no message not used for
   * this purpose should be named `computer.signal`.
   * <p/>
   * Note that message handlers may also return results. In this case that
   * result will be returned from this function. In the case that there are
   * more than one target node (shared addresses, should not happen, but may if
   * a node implementation decides to ignore this rule) the last result that
   * was not `None` will be returned, or `None` if all results were `None`.
   *
   * @param source the node that sends the message.
   * @param target the id of the node to send the message to.
   * @param name   the name of the message.
   * @param data   the message to send.
   * @return the result of the message being handled, if any.
   * @throws IllegalArgumentException if the source node is not in this network.
   */
  def sendToAddress(source: Node, target: String, name: String, data: Any*): Option[Array[Any]]

  /**
   * Sends a message to all addressed, visible neighbors of the source node.
   * <p/>
   * Targets are determined using `neighbors(source)` and additionally filtered
   * for visibility (so that nodes with `Visibility.None` are ignored).
   * <p/>
   * Messages should have a unique name to allow differentiating them when
   * handling them in a network node. For example, computers will try to parse
   * messages named `computer.signal` by converting the message data to a
   * signal and inject that signal into the Lua VM, so no message not used for
   * this purpose should be named `computer.signal`.
   *
   * @param source the node that sends the message.
   * @param name   the name of the message.
   * @param data   the message to send.
   * @throws IllegalArgumentException if the source node is not in this network.
   * @see `neighbors`
   */
  def sendToNeighbors(source: Node, name: String, data: Any*)

  /**
   * Sends a message to all addressed nodes visible to the source node.
   * <p/>
   * Targets are determined using `nodes(source)`.
   * <p/>
   * Messages should have a unique name to allow differentiating them when
   * handling them in a network node. For example, computers will try to parse
   * messages named `computer.signal` by converting the message data to a
   * signal and inject that signal into the Lua VM, so no message not used for
   * this purpose should be named `computer.signal`.
   *
   * @param source the node that sends the message.
   * @param data   the message to send.
   * @throws IllegalArgumentException if the source node is not in this network.
   * @see `nodes`
   */
  def sendToVisible(source: Node, name: String, data: Any*)
}

object Network extends NetworkAPI {
  /**
   * Tries to add a tile entity network node at the specified coordinates to
   * adjacent networks.
   *
   * @param world the world the tile entity lives in.
   * @param x     the X coordinate of the tile entity.
   * @param y     the Y coordinate of the tile entity.
   * @param z     the Z coordinate of the tile entity.
   */
  def joinOrCreateNetwork(world: IBlockAccess, x: Int, y: Int, z: Int) =
    instance.foreach(_.joinOrCreateNetwork(world, x, y, z))

  // ----------------------------------------------------------------------- //

  /** Initialized in pre-init. */
  private[oc] var instance: Option[NetworkAPI] = None
}
