package li.cil.oc.server.component

import li.cil.oc.api.network.Message
import li.cil.oc.api.network.environment.ManagedEnvironment
import net.minecraft.nbt.NBTTagCompound
import scala.math.ScalaNumber

abstract class ManagedComponent extends ManagedEnvironment {
  def update() {}

  def onMessage(message: Message) = null

  def onDisconnect() {}

  def onConnect() {}

  def load(nbt: NBTTagCompound) {}

  def save(nbt: NBTTagCompound) {}

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
