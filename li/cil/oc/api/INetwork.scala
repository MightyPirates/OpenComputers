package li.cil.oc.api

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
 * one. All this logic is provided by `NetworkAPI#joinOrCreateNetwork`.
 * <p/>
 * Note that for network nodes implemented in <tt>TileEntities</tt> adding and
 * removal is automatically provided on chunk load and unload. When a block is
 * placed or broken you will have to implement this logic yourself (i.e. call
 * <tt>NetworkAPI.joinOrCreateNetwork</tt> in <tt>onBlockAdded</tt> and
 * <tt>getNetwork.remove</tt> in <tt>breakBlock</tt>.
 * <p/>
 * All other kinds of nodes have to be managed manually. See `INetworkNode`.
 * <p/>
 * There are a couple of system messages to be aware of. These are all sent by
 * the network manager itself:
 * <ul>
 * <li><tt>network.connect</tt> is generated when a node is added to the
 * network, with the added node as the sender.</li>
 * <li><tt>network.disconnect</tt> is generated when a node is removed from the
 * network, with the removed node as the sender.</li>
 * <li><tt>network.reconnect</tt> is generated when a node's address changes,
 * usually due to a network merge, with the node whose address changed as the
 * sender and the old address as the only parameter.</li>
 * </ul>
 * <p/>
 * IMPORTANT: do <em>not</em> implement this interface yourself and create
 * instances of your own network implementation; this will lead to
 * incompatibilities with the built-in network implementation (which can only
 * merge with other networks of its own type). Always use the methods provided
 * in <tt>NetworkAPI</tt> to create and join networks.
 */
trait INetwork {
  /**
   * Adds a new node connection in the network.
   * <p/>
   * This is used by nodes to join an existing network. At least one of the two
   * nodes must already be in the network. If one of the nodes is not yet in the
   * network, it will be added to the network. If both nodes are already in the
   * network only the connection between the two nodes is stored. If one of the
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
   * @throws IllegalStateException    if this is called while the network is
   *                                  already updating, for example from
   *                                  `INetworkNode#receive` (which may be
   *                                  called during the update).
   */
  def connect(nodeA: INetworkNode, nodeB: INetworkNode): Boolean

  /**
   * Removes a node connection in the network.
   * <p/>
   * Both nodes must be part of the same network.
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
  def disconnect(nodeA: INetworkNode, nodeB: INetworkNode): Boolean

  /**
   * Removes a node from the network.
   * <p/>
   * This should be called by nodes when they are destroyed (e.g. onBreakBlock)
   * or unloaded. If removing the node leads to two graphs (it was the a bridge
   * node) the network will be split up.
   *
   * @param node the node to remove from the network.
   * @return whether the node was removed.
   */
  def remove(node: INetworkNode): Boolean

  /**
   * Get the network node with the specified address.
   * <p/>
   * If there are multiple nodes with the same address this will return the
   * node that most recently joined the network.
   *
   * @param address the address of the node to get.
   * @return the node with that address.
   */
  def node(address: Int): Option[INetworkNode]

  /**
   * The list of nodes in this network.
   * <p/>
   * This can be used to perform a delayed initialization of a node. For
   * example, computers will use this when starting up to generate component
   * added events for all nodes.
   *
   * @return the list of nodes in this network.
   */
  def nodes: Iterable[INetworkNode]

  /**
   * The list of nodes the specified node is directly connected to.
   * <p/>
   * This can be used to verify arguments for components that should only work
   * for other components that are directly connected to them, for example.
   *
   * @param node the node to get the neighbors for.
   * @return a list of nodes the node is directly connect to.
   * @throws IllegalArgumentException if the specified node is not in this network.
   */
  def neighbors(node: INetworkNode): Iterable[INetworkNode]

  /**
   * Sends a message to a specific node.
   * <p/>
   * Messages should have a unique name to allow differentiating them when
   * handling them in a network node. For example, computers will try to parse
   * messages named "computer.signal" by converting the message data to a
   * signal and inject that signal into the Lua VM, so no message not used for
   * this purpose should be named "computer.signal".
   * <p/>
   * Note that message handlers may also return results. In this case that
   * result will be returned from this function. In the case that there are
   * more than one target node (shared addresses) the last result that was not
   * `None` will be returned, or `None` if all were.
   *
   * @param source the node that sends the message.
   * @param target the id of the node to send the message to.
   * @param name   the name of the message.
   * @param data   the message to send.
   * @return the result of the message being handled, if any.
   */
  def sendToNode(source: INetworkNode, target: Int, name: String, data: Any*): Option[Array[Any]]

  /**
   * Sends a message to all nodes in the network.
   * <p/>
   * Messages should have a unique name to allow differentiating them when
   * handling them in a network node. For example, computers will try to parse
   * messages named "computer.signal" by converting the message data to a
   * signal and inject that signal into the Lua VM, so no message not used for
   * this purpose should be named "computer.signal".
   *
   * @param source the node that sends the message.
   * @param data   the message to send.
   */
  def sendToAll(source: INetworkNode, name: String, data: Any*)
}