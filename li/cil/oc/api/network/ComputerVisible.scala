package li.cil.oc.api.network

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.relauncher.Side
import li.cil.oc.OpenComputers
import net.minecraft.nbt.NBTTagCompound

/**
 * This trait may be used in nodes that should be visible to computers.
 * <p/>
 * Computer "visibility" may have to differ from real network visibility in
 * some cases, such as network cards (which have to be able to communicate
 * across the whole network, but a computer should only "see" the card
 * installed in itself).
 * <p/>
 * Unlike the node's network visibility, this is a dynamic value and can be
 * changed at any time. For example, this is used to hide multi-block screen
 * parts that are not the origin from computers in the network.
 * <p/>
 * The method responsible for dispatching network messages from computers also
 * checks this flag, so nodes won't receive messages from computer's that
 * should not be able to see them.
 */
trait ComputerVisible extends Node {
  private var visibility_ = Visibility.None

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
  def computerVisibility = visibility_

  def computerVisibility_=(value: Visibility.Value) = {
    val newVisibility = if (value > visibility) {
      OpenComputers.log.warning("Trying to set computer visibility to '" + value + "' on a node with visibility '" +
        visibility + "'. It will be limited to the node's visibility.")
      visibility
    }
    else value
    if (newVisibility != visibility_ && FMLCommonHandler.instance.getEffectiveSide == Side.SERVER) {
      visibility_ match {
        case Visibility.Neighbors => newVisibility match {
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
        case Visibility.Network => newVisibility match {
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
        case Visibility.None => newVisibility match {
          case Visibility.Neighbors => network.foreach(_.sendToNeighbors(this, "computer.signal", "component_added"))
          case Visibility.Network => network.foreach(_.sendToVisible(this, "computer.signal", "component_added"))
          case _ => // Cannot happen, but avoids compiler warnings.
        }
      }
      visibility_ = newVisibility
    }
    this
  }

  /**
   * Tests whether this node can be seen by the specified node, representing a
   * computer in the network.
   *
   * @param computer the computer node to check for.
   * @return true if the computer can see this node; false otherwise.
   */
  def canBeSeenBy(computer: Node) = computerVisibility match {
    case Visibility.None => false
    case Visibility.Network => true
    case Visibility.Neighbors => network.exists(_.neighbors(computer).exists(_ == this))
  }

  // ----------------------------------------------------------------------- //

  override def receive(message: Message) = super.receive(message) orElse {
    message.name match {
      case "computer.started" if canBeSeenBy(message.source) =>
        network.get.sendToAddress(this, message.source.address.get, "computer.signal", "component_added")
      case _ => None
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    if (nbt.hasKey("computerVisibility"))
      visibility_ = Visibility(nbt.getInteger("computerVisibility"))
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setInteger("computerVisibility", visibility_.id)
  }
}
