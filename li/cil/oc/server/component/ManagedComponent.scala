package li.cil.oc.server.component

import li.cil.oc.api.network.{ManagedEnvironment, Node, Message}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.NBTTagCompound
import scala.math.ScalaNumber

abstract class ManagedComponent extends ManagedEnvironment {
  def update() {}

  def onMessage(message: Message) {}

  def onDisconnect(node: Node) {}

  def onConnect(node: Node) {}

  def load(nbt: NBTTagCompound) = {
    if (node != null) node.load(nbt.getCompoundTag("node"))
  }

  def save(nbt: NBTTagCompound) = {
    if (node != null) nbt.setNewCompoundTag("node", node.save)
  }

  final protected def result(args: Any*): Array[AnyRef] = {
    def unwrap(arg: Any): AnyRef = arg match {
      case x: ScalaNumber => x.underlying
      case x => x.asInstanceOf[AnyRef]
    }
    Array(args map unwrap: _*)
  }
}
