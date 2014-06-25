package li.cil.oc.common.component

import li.cil.oc.api
import li.cil.oc.api.network.{ManagedEnvironment, Message, Node}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.NBTTagCompound

import scala.math.ScalaNumber

abstract class ManagedComponent extends ManagedEnvironment {
  val canUpdate = false

  override def update() {}

  override def onConnect(node: Node) {}

  override def onDisconnect(node: Node) {}

  override def onMessage(message: Message) {}

  override def load(nbt: NBTTagCompound) = {
    if (node != null) node.load(nbt.getCompoundTag("node"))
  }

  override def save(nbt: NBTTagCompound) = {
    // Force joining a network when saving and we're not in one yet, so that
    // the address is embedded in the saved data that gets sent to the client,
    // so that that address can be used to associate components on server and
    // client (for example keyboard and screen/text buffer).
    if (node != null) {
      if (node.address == null) {
        api.Network.joinNewNetwork(node)
        nbt.setNewCompoundTag("node", node.save)
        node.remove()
      }
      else {
        nbt.setNewCompoundTag("node", node.save)
      }
    }
  }

  final protected def result(args: Any*): Array[AnyRef] = {
    def unwrap(arg: Any): AnyRef = arg match {
      case x: ScalaNumber => x.underlying
      case x => x.asInstanceOf[AnyRef]
    }
    Array(args map unwrap: _*)
  }
}
