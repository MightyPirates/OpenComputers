package li.cil.oc.api.network

import li.cil.oc.api.{Persistable, Network}
import net.minecraft.nbt.{NBTTagString, NBTTagCompound}
import scala.math.ScalaNumber

/**
 * A single node in a `INetwork`.
 * <p/>
 * All nodes in a network <em>must</em> have a unique address; the network will
 * generate a unique address and assign it to new nodes. A node must never ever
 * change its address on its own accord.
 * <p/>
 * Per default there are two kinds of nodes: tile entities and item components.
 * If a `TileEntity` implements this interface adding/removal from its
 * network on load/unload will automatically be handled and you will only have
 * to ensure it is added/removed to/from a network when the corresponding block
 * is added/removed. Item components in containers have to be handled manually.
 * All other kinds of nodes you may come up with will also have to be handled
 * manually.
 * <p/>
 * Items have to be handled by a corresponding `ItemDriver`. Existing
 * blocks may be interfaced with a proxy block if a `BlockDriver` exists
 * that supports the block.
 */
trait Node extends Persistable {
  /**
   * The name of the node.
   * <p/>
   * This should be the type name of the component represented by the node,
   * since this is what is returned from `driver.componentType`. As such it
   * is to be expected that there be multiple nodes with the same name.
   */
  val name: String

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
   */
  val visibility: Visibility.Value

  /**
   * The visibility of this node when it comes to computers.
   * <p/>
   * This is used to decide for which components to generate `component_added`
   * and `component_removed` signals in computers when they are added and
   * removed from the network, respectively.
   * <p/>
   * For example, a network card should be visible to the entire network so
   * that it can receive messages from network cards in other computers, but
   * other computers that the one it is plugged into should not treat them as
   * components added to them, since that would be silly, meaning this field
   * will be set to neighbors only for them.
   * <p/>
   * Another example would power distributors, which should also be visible
   * to the entire network, but always be invisible to computers, so their
   * value for this field will be `Visibility.None`.
   */
  lazy val computerVisibility: Visibility.Value = visibility

  // ----------------------------------------------------------------------- //

  /**
   * The address of the node, so that it can be found in the network.
   * <p/>
   * This is used by the network manager when a node is added to a network to
   * assign it a unique address, if it doesn't already have one. Nodes must not
   * use custom addresses, only those assigned by the network. The only option
   * they have is to *not* have an address, which can be useful for "dummy"
   * nodes, such as cables. In that case they may ignore the address being set.
   */
  var address: Option[String] = None

  /**
   * The network this node is currently in.
   * <p/>
   * Note that valid nodes should never return `None` here. When created a node
   * should immediately be added to a network, after being removed from its
   * network a node should be considered invalid.
   * <p/>
   * This will always be set automatically by the network manager. Do not
   * change this value and do not return anything that it wasn't set to.
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
      case "system.connect" => onConnect()
      case "system.disconnect" => onDisconnect()
      case _ => // Ignore.
    }
    None
  }

  /**
   * This is called once per tick, if the node is owned either by a computer
   * (meaning it's an item component installed in a computer) or is managed by
   * an adapter (meaning it was acquired via a block driver for a block whose
   * tile entity is not a node).
   * <p/>
   * For nodes implemented directly in tile entities you should just call this
   * from the tile entity's `updateEntity` function yourself, as necessary.
   * <p/>
   * When implementing an "inventory", i.e. something that holds item
   * components, be sure to call `update` for the installed components' nodes.
   */
  def update() {}

  // ----------------------------------------------------------------------- //

  /**
   * Reads a previously stored address value from the specified tag.
   * <p/>
   * This should be called when associated object is loaded. For items, this
   * should be called when their container is loaded. For blocks this should
   * be called when their tile entity is loaded.
   *
   * @param nbt the tag to read from.
   */
  def load(nbt: NBTTagCompound) = {
    if (nbt.hasKey("address") && nbt.getTag("address").isInstanceOf[NBTTagString])
      address = Option(nbt.getString("address"))
  }

  /**
   * Stores the node's address in the specified NBT tag, to keep addresses the
   * same across unloading/loading.
   * <p/>
   * This should be called when the implementing class is saved. For items,
   * this should be called when their container is saved. For blocks this
   * should be called when their tile entity is saved.
   *
   * @param nbt the tag to write to.
   */
  def save(nbt: NBTTagCompound) = {
    address.foreach(nbt.setString("address", _))
  }

  // ----------------------------------------------------------------------- //

  /**
   * Called when this node is added to a network.
   * <p/>
   * Use this for custom initialization logic.
   */
  protected def onConnect() {}

  /**
   * Called when this node is removed from a network.
   * <p/>
   * Use this for custom tear-down logic. A node should be considered invalid
   * and non-reusable after this has happened.
   */
  protected def onDisconnect() {}

  // ----------------------------------------------------------------------- //

  /**
   * Handy function for returning a list of results.
   *
   * @param args the values to return.
   * @return and array option as required by `receive`.
   */
  protected def result(args: Any*): Option[Array[Any]] = {
    def unwrap(arg: Any): AnyRef = arg match {
      case x: ScalaNumber => x.underlying
      case x => x.asInstanceOf[AnyRef]
    }
    Some(Array(args map unwrap: _*))
  }
}