package li.cil.oc.api.network

import li.cil.oc.api.{Persistable, Network}
import net.minecraft.nbt.NBTTagCompound

/**
 * A single node in a `INetwork`.
 * <p/>
 * All nodes in a network <em>should</em> have a unique address; the network
 * will try to generate a unique address and assign it to new nodes. A node must
 * never ever change its address while in a network (because the lookup-table in
 * the network manager would not be notified of this change). If you must change
 * the address, use `Network.reconnect`.
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
trait Node extends Persistable {
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
   * The visibility of this node.
   * <p/>
   * This is used by the network to control which system messages to deliver to
   * which nodes. This value should not change over the lifetime of a node.
   * Note that this has no effect on the real reachability of a node; it is
   * only used to filter to which nodes to send connect, disconnect and
   * reconnect messages. If addressed directly or when a broadcast is sent, the
   * node will still receive that message. Therefore nodes should still verify
   * themselves that they want to accept a message from the message's source.
   *
   * @return visibility of the node.
   */
  def visibility = Visibility.None

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
  var address = 0

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
  var network: Option[Network] = None

  /**
   * Makes the node handle a message.
   * <p/>
   * Some messages may expect a result. In this case the handler function may
   * return that result. If multiple handlers are executed, the last result
   * that was not `None` will be used, if any. Otherwise the overall result
   * will also be `None`.
   * <p/>
   * Note that you should always call `super.receive()` in your implementation
   * since this also is used to trigger the `onConnect`, `onDisconnect` and
   * `onAddressChange` functions.
   *
   * @param message the message to handle.
   * @return the result of the message being handled, if any.
   */
  def receive(message: Message): Option[Array[Any]] = {
    if (message.source == this) message.name match {
      case "network.connect" => onConnect()
      case "network.disconnect" => onDisconnect()
      case "network.reconnect" => onReconnect()
      case _ => // Ignore.
    }
    None
  }

  /**
   * Reads a previously stored address value from the specified tag.
   *
   * This should be called when implementing class is loaded.
   *
   * @param nbt the tag to read from.
   */
  def load(nbt: NBTTagCompound) = {
    network match {
      case None => address = nbt.getInteger("address")
      case Some(net) => net.reconnect(this, nbt.getInteger("address"))
    }
  }

  /**
   * Stores the node's address in the specified NBT tag, to keep addresses the
   * same across unloading/loading.
   *
   * This should be called when the implementing class is saved.
   *
   * @param nbt the tag to write to.
   */
  def save(nbt: NBTTagCompound) = nbt.setInteger("address", address)

  protected def onConnect() {}

  protected def onDisconnect() {}

  protected def onReconnect() {}
}