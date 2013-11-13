package li.cil.oc.server.network

import li.cil.oc.Config
import li.cil.oc.api.network
import li.cil.oc.util.Persistable
import net.minecraft.nbt.NBTTagCompound

trait Connector extends network.Connector with Persistable {
  val bufferSize: Double

  var dirty = true

  var buffer = 0.0

  def changeBuffer(delta: Double) = if (delta != 0) {
    val oldBuffer = buffer
    buffer = buffer + delta
    val ok = if (buffer < 0) {
      buffer = 0
      false
    }
    else if (buffer > bufferSize) {
      buffer = bufferSize
      false
    }
    else true
    if (buffer != oldBuffer) dirty = true
    ok || Config.ignorePower
  } else true

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    buffer = nbt.getDouble(Config.namespace + "connector.buffer") max 0 min bufferSize
    dirty = true
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setDouble(Config.namespace + "connector.buffer", buffer)
  }
}
