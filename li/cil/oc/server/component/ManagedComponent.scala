package li.cil.oc.server.component

import li.cil.oc.api.network.{ManagedEnvironment, Node, Message}
import net.minecraft.nbt.NBTTagCompound
import scala.math.ScalaNumber

abstract class ManagedComponent extends ManagedEnvironment {
  def update() {}

  def onMessage(message: Message) {}

  def onDisconnect(node: Node) {}

  def onConnect(node: Node) {}

  def load(nbt: NBTTagCompound) = {
    if (node != null) node.load(nbt)
  }

  def save(nbt: NBTTagCompound) = {
    if (node != null) node.save(nbt)
  }

  /**
   * Handy function for returning a list of results.
   * <p/>
   * This is primarily meant to be used for returning result arrays from Lua
   * callbacks, to avoid having to write `XYZ.box(...)` all the time.
   *
   * @param args the values to return.
   * @return and array of objects.
   */
  final protected def result(args: Any*): Array[Object] = {
    def unwrap(arg: Any): AnyRef = arg match {
      case x: ScalaNumber => x.underlying
      case x => x.asInstanceOf[AnyRef]
    }
    Array(args map unwrap: _*)
  }
}
