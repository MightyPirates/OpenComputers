package li.cil.oc.api

/**
 * A single node in a `INetwork`.
 * <p/>
 * All nodes in a network <em>should</em> have a unique address; the network
 * will try to generate a unique address and assign it to new nodes. A node must
 * never ever change its address while in a network (because the lookup-table in
 * the network manager would not be notified of this change). If you must change
 * the address, remove the node first, change the address and then add it again.
 * <p/>
 * Per default there are two kinds of nodes: tile entities and item components.
 * If a `TileEntity` implements this interface adding/removal from its
 * network on load/unload will automatically be handled and you will only have
 * to ensure it is added/removed to/from a network when the corresponding block
 * is added/removed. Item components in containers have to be handled manually.
 * All other kinds of nodes you may come up with will also have to be handled
 * manually.
 * <p/>
 * Items have to be handled by a corresponding `IItemDriver`. Existing
 * blocks may be interfaced with a proxy block if a `IBlockDriver` exists
 * that supports the block.
 */
trait INetworkNode {
  /**
   * The name of the node.
   * <p/>
   * This should be the type name of the component represented by the node,
   * since this is what is returned from `driver.componentType`. As such it
   * is to be expected that there be multiple nodes with the same name.
   *
   * @return the name of the node.
   */
  def name: String

  /**
   * The address of the node, so that it can be found in the network.
   * <p/>
   * This is used by the network manager when a node is added to a network to
   * assign it a unique address in that network. Nodes should avoid using custom
   * addresses since that could lead to multiple nodes with the same address in
   * a network. Note that that in and by itself is supported however, it is just
   * harder to work with.
   * <p/>
   * Some nodes may however wish to simply ignore this and always provide the
   * same address (e.g. zero), when they are never expected to be used by other
   * nodes (cables, for example).
   *
   * @return the id of this node.
   */
  def address = address_

  def address_=(value: Int) = if (value != address_) {
    address_ = value
    onAddressChange()
    this
  }

  /**
   * The network this node is currently in.
   * <p/>
   * Note that valid nodes should never return `null` here. When created a node
   * should immediately be added to a network, after being removed from its
   * network it should be considered invalid.
   * <p/>
   * This is used by the `NetworkAPI` and the network itself when merging with
   * another network. You should never have to set this yourself.
   *
   * @return the network the node is in.
   */
  def network = network_.orNull

  def network_=(n: INetwork) = {
    if (network_.isDefined) {
      network_ = None
      onDisconnect()
    }
    network_ = Option(n)
    if (network_.isDefined) {
      onConnect()
    }
    this
  }

  /**
   * Makes the node handle a message.
   *
   * Some messages may expect a result. In this case the handler function may
   * return that result. If multiple handlers are executed, the last result
   * that was not `None` will be used, if any. Otherwise the overall result
   * will also be `None`.
   *
   * @param message the message to handle.
   * @return the result of the message being handled, if any.
   */
  def receive(message: INetworkMessage): Option[Array[Any]] = None

  def onConnect() {}

  def onDisconnect() {}

  def onAddressChange() {}

  private var network_ = None: Option[INetwork]

  private var address_ = 0
}