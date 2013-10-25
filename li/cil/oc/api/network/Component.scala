package li.cil.oc.api.network

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.relauncher.Side
import net.minecraft.nbt.NBTTagCompound

/**
 * Components are nodes that can be addressed computers via drivers.
 * <p/>
 * Components therefore form a sub-network in the overall network, and some
 * special rules apply to them. For one, components specify an additional
 * kind of visibility. Component visibility may have to differ from real
 * network visibility in some cases, such as network cards (which have to
 * be able to communicate across the whole network, but computers should only
 * "see" the cards installed directly in them).
 * <p/>
 * Unlike the `Node`'s network visibility, this is a dynamic value and can be
 * changed at any time. For example, this is used to hide multi-block screen
 * parts that are not the origin from computers in the network.
 * <p/>
 * The method responsible for dispatching network messages from computers also
 * only allows sending messages to components that the computer can see,
 * according to the component's visibility. Therefore nodes won't receive
 * messages from computer's that should not be able to see them.
 */
trait Component extends Node {
  private var visibility_ = Visibility.None

  /**
   * The visibility of this component.
   * <p/>
   * Note that this cannot be higher / more visible than the visibility of the
   * node. Trying to set it to a higher value will generate an exception.
   */
  def componentVisibility = visibility_

  def componentVisibility_=(value: Visibility) = {
    if (value.ordinal() > visibility.ordinal()) {
      throw new IllegalArgumentException("Trying to set computer visibility to '" + value + "' on a '" + name +
        "' node with visibility '" + visibility + "'. It will be limited to the node's visibility.")
    }
    if (value != visibility_ && FMLCommonHandler.instance.getEffectiveSide == Side.SERVER) {
      visibility_ match {
        case Visibility.Neighbors => value match {
          case Visibility.Network =>
            network.foreach(network => {
              val neighbors = network.neighbors(this).toSet
              val visible = network.nodes(this)
              val delta = visible.filterNot(neighbors.contains)
              delta.foreach(node => network.sendToAddress(this, node.address.get, "computer.signal", "component_added"))
            })
          case Visibility.None => network.foreach(_.sendToNeighbors(this, "computer.signal", "component_removed"))
          case _ => // Cannot happen, but avoids compiler warnings.
        }
        case Visibility.Network => value match {
          case Visibility.Neighbors =>
            network.foreach(network => {
              val neighbors = network.neighbors(this).toSet
              val visible = network.nodes(this)
              val delta = visible.filterNot(neighbors.contains)
              delta.foreach(node => network.sendToAddress(this, node.address.get, "computer.signal", "component_removed"))
            })
          case Visibility.None => network.foreach(_.sendToVisible(this, "computer.signal", "component_removed"))
          case _ => // Cannot happen, but avoids compiler warnings.
        }
        case Visibility.None => value match {
          case Visibility.Neighbors => network.foreach(_.sendToNeighbors(this, "computer.signal", "component_added"))
          case Visibility.Network => network.foreach(_.sendToVisible(this, "computer.signal", "component_added"))
          case _ => // Cannot happen, but avoids compiler warnings.
        }
      }
      visibility_ = value
    }
    this
  }

  /**
   * Tests whether this component can be seen by the specified node,
   * usually representing a computer in the network.
   *
   * @param other the computer node to check for.
   * @return true if the computer can see this node; false otherwise.
   */
  def canBeSeenBy(other: Node) = componentVisibility match {
    case Visibility.None => false
    case Visibility.Network => true
    case Visibility.Neighbors => other.network.exists(_.neighbors(other).exists(_ == this))
  }

  // ----------------------------------------------------------------------- //

  override def receive(message: Message) = Option(super.receive(message)).orElse {
    if (message.name == "computer.started" && canBeSeenBy(message.source))
      Some(network.get.sendToAddress(this, message.source.address.get, "computer.signal", "component_added"))
    else None
  }.orNull

  // ----------------------------------------------------------------------- //

  override abstract def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    if (nbt.hasKey("componentVisibility"))
      visibility_ = Visibility.values()(nbt.getInteger("componentVisibility"))
  }

  override abstract def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    nbt.setInteger("componentVisibility", visibility_.ordinal())
  }
}
