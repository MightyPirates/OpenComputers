package li.cil.oc.server.component

import java.io.{FileNotFoundException, IOException}
import li.cil.oc.api
import li.cil.oc.api.Network
import li.cil.oc.api.fs.Mode
import li.cil.oc.api.network.Message
import net.minecraft.nbt.NBTTagCompound
import scala.collection.mutable

class FileSystem(val fileSystem: api.FileSystem) extends ItemComponent {

  private val handles = mutable.Map.empty[String, mutable.Set[Long]]

  override def name = "fs"

  override def receive(message: Message) = {
    message.data match {
      case Array() if message.name == "network.disconnect" && handles.contains(message.source.address.get) =>
        for (handle <- handles(message.source.address.get)) {
          fileSystem.file(handle) match {
            case None => // Maybe file system was accessed from somewhere else.
            case Some(file) => file.close()
          }
        }
      case _ => // Ignore.
    }
    super.receive(message)
  }

  override protected def receiveFromNeighbor(network: Network, message: Message) =
    try {
      message.data match {
        case Array(path: Array[Byte]) if message.name == "fs.exists" =>
          Some(Array(fileSystem.exists(clean(path)).asInstanceOf[Any]))
        case Array(path: Array[Byte]) if message.name == "fs.exists" =>
          Some(Array(fileSystem.size(clean(path)).asInstanceOf[Any]))
        case Array(path: Array[Byte]) if message.name == "fs.isDirectory" =>
          Some(Array(fileSystem.isDirectory(clean(path)).asInstanceOf[Any]))
        case Array(path: Array[Byte]) if message.name == "fs.list" =>
          fileSystem.list(clean(path)) match {
            case Some(list) => Some(list.map(_.asInstanceOf[Any]))
            case _ => None
          }
        case Array(path: Array[Byte]) if message.name == "fs.remove" =>
          Some(Array(fileSystem.remove(clean(path)).asInstanceOf[Any]))
        case Array(from: Array[Byte], to: Array[Byte]) if message.name == "fs.rename" =>
          Some(Array(fileSystem.rename(clean(from), clean(to)).asInstanceOf[Any]))
        case Array(path: Array[Byte], mode: Array[Byte]) if message.name == "fs.open" =>
          val handle = fileSystem.open(clean(path), Mode.parse(new String(mode, "UTF-8")))
          if (handle > 0) {
            handles.getOrElseUpdate(message.source.address.get, mutable.Set.empty[Long]) += handle
          }
          Some(Array(handle.asInstanceOf[Any]))

        case Array(handle: Double) if message.name == "fs.close" =>
          fileSystem.file(handle.toLong) match {
            case None => // Ignore.
            case Some(file) =>
              handles.get(message.source.address.get) match {
                case None => // Not the owner of this handle.
                case Some(set) => if (set.remove(handle.toLong)) file.close()
              }
          }
          None
        case Array(handle: Double, n: Double) if message.name == "fs.read" && n > 0 =>
          fileSystem.file(handle.toLong) match {
            case None => None
            case Some(file) =>
              // Limit reading to chunks of 8KB to avoid crazy allocations.
              val buffer = new Array[Byte](n.toInt min 8192)
              val read = file.read(buffer)
              if (read >= 0) {
                val result = new Array[Byte](read)
                Array.copy(buffer, 0, result, 0, read)
                Some(Array(result))
              }
              else {
                Some(Array(Unit))
              }
          }
        case Array(handle: Double, whence: Array[Byte], offset: Double) if message.name == "fs.seek" =>
          fileSystem.file(handle.toLong) match {
            case None => None
            case Some(file) => None // TODO
          }
        case Array(handle: Double, value: Array[Byte]) if message.name == "fs.write" =>
          fileSystem.file(handle.toLong) match {
            case None => None
            case Some(file) => file.write(value); Some(Array(true.asInstanceOf[Any]))
          }
        case _ => None
      }
    } catch {
      case e@(_: IOException | _: IllegalArgumentException) => Some(Array(Unit, e.getMessage))
    }

  private def clean(path: Array[Byte]) = {
    val result = com.google.common.io.Files.simplifyPath(new String(path, "UTF-8"))
    if (result.startsWith("../")) throw new FileNotFoundException(result)
    if (result == "/" || result == ".") ""
    else result
  }

  override protected def onDisconnect() {
    super.onDisconnect()
    fileSystem.close()
  }

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)

    fileSystem.load(nbt.getCompoundTag("fs"))
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)

    val fsNbt = new NBTTagCompound()
    fileSystem.save(fsNbt)
    nbt.setCompoundTag("fs", fsNbt)
  }
}