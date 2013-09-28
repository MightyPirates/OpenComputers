package li.cil.oc.client.computer

import li.cil.oc.common.component
import net.minecraft.nbt.NBTTagCompound

/**
 * This is a dummy class for the client side. It does nothing, really, just
 * saves us a couple of side checks.
 */
class Computer(owner: Any) extends component.Computer {
  override def start() = false

  override def stop() = false

  var isRunning = false

  override def update() {}

  override def signal(name: String, args: Any*) = ???

  override def load(nbt: NBTTagCompound) {}

  override def save(nbt: NBTTagCompound) {}
}