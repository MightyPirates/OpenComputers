package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.Network
import li.cil.oc.api.network.Message
import net.minecraft.nbt.NBTTagCompound

class FileSystem(val fileSystem: api.FileSystem, val nbt: NBTTagCompound) extends ItemComponent {
  override def name = "disk"

  override protected def receiveFromNeighbor(network: Network, message: Message) = message.data match {
    case Array(dir: String) if message.name == "disk.ls" =>
      fileSystem.list(clean(dir)) match {
        case Some(list) => Some(list.map(_.asInstanceOf[Any]))
        case _ => None
      }
    case Array(path: String) if message.name == "disk.rm" =>
      Some(Array(fileSystem.remove(clean(path)).asInstanceOf[Any]))
    case Array(from: String, to: String) if message.name == "disk.mv" =>
      Some(Array(fileSystem.rename(clean(from), clean(to)).asInstanceOf[Any]))
    case _ => None
  }

  // TODO
  private def clean(path: String) = path
}
