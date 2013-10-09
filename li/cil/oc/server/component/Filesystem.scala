package li.cil.oc.server.component

import java.io.{FileNotFoundException, IOException}
import li.cil.oc.api
import li.cil.oc.api.fs.Mode
import li.cil.oc.api.network.{Node, Visibility, Message}
import net.minecraft.nbt.{NBTTagInt, NBTTagList, NBTTagCompound}
import scala.collection.mutable

class FileSystem(val fileSystem: api.FileSystem) extends Node {
  private val owners = mutable.Map.empty[String, mutable.Set[Int]]

  private var label = ""

  override def name = "filesystem"

  override def visibility = Visibility.Neighbors

  override def receive(message: Message) = super.receive(message).orElse {
    try {
      message.data match {
        case Array() if message.name == "network.disconnect" && owners.contains(message.source.address.get) =>
          for (handle <- owners(message.source.address.get)) {
            fileSystem.file(handle) match {
              case None => // Maybe file system was accessed from somewhere else.
              case Some(file) => file.close()
            }
          }
          None
        case Array() if message.name == "computer.stopped" =>
          owners.get(message.source.address.get) match {
            case None => // Computer had no open files.
            case Some(set) =>
              set.foreach(handle => fileSystem.file(handle) match {
                case None => // Invalid handle... huh.
                case Some(file) => file.close()
              })
              set.clear()
          }
          None

        case Array(label: Array[Byte]) if message.name == "fs.label=" =>
          this.label = new String(label, "UTF-8").trim
          if (this.label.length > 16)
            this.label = this.label.substring(0, 16)
          result(true)
        case Array() if message.name == "fs.label" =>
          result(label)

        case Array() if message.name == "fs.spaceTotal" =>
          val space = fileSystem.spaceTotal
          if (space < 0)
            result("unlimited")
          else
            result(space)
        case Array() if message.name == "fs.spaceUsed" =>
          result(fileSystem.spaceUsed)

        case Array(path: Array[Byte]) if message.name == "fs.exists" =>
          result(fileSystem.exists(clean(path)))
        case Array(path: Array[Byte]) if message.name == "fs.size" =>
          result(fileSystem.size(clean(path)))
        case Array(path: Array[Byte]) if message.name == "fs.isDirectory" =>
          result(fileSystem.isDirectory(clean(path)))
        case Array(path: Array[Byte]) if message.name == "fs.dir" =>
          fileSystem.list(clean(path)) match {
            case Some(list) => Some(list.map(_.asInstanceOf[Any]))
            case _ => None
          }

        case Array(path: Array[Byte]) if message.name == "fs.makeDirectory" =>
          result(fileSystem.makeDirectories(clean(path)))
        case Array(path: Array[Byte]) if message.name == "fs.remove" =>
          result(fileSystem.remove(clean(path)))
        case Array(from: Array[Byte], to: Array[Byte]) if message.name == "fs.rename" =>
          result(fileSystem.rename(clean(from), clean(to)))

        case Array(handle: Double) if message.name == "fs.close" =>
          fileSystem.file(handle.toInt) match {
            case None => // Ignore.
            case Some(file) =>
              owners.get(message.source.address.get) match {
                case None => // Not the owner of this handle.
                case Some(set) => if (set.remove(handle.toInt)) file.close()
              }
          }
          None
        case Array(path: Array[Byte], mode: Array[Byte]) if message.name == "fs.open" =>
          val handle = fileSystem.open(clean(path), Mode.parse(new String(mode, "UTF-8")))
          if (handle > 0) {
            owners.getOrElseUpdate(message.source.address.get, mutable.Set.empty[Int]) += handle
          }
          result(handle)

        case Array(handle: Double, n: Double) if message.name == "fs.read" && n > 0 =>
          fileSystem.file(handle.toInt) match {
            case None => throw new IOException("bad file descriptor")
            case Some(file) =>
              // Limit reading to chunks of 8KB to avoid crazy allocations.
              val buffer = new Array[Byte](n.toInt min (8 * 1024))
              val read = file.read(buffer)
              if (read >= 0) {
                val bytes =
                  if (read == buffer.length)
                    buffer
                  else {
                    val bytes = new Array[Byte](read)
                    Array.copy(buffer, 0, bytes, 0, read)
                    bytes
                  }
                result(bytes)
              }
              else {
                result(Unit)
              }
          }
        case Array(handle: Double, whence: Array[Byte], offset: Double) if message.name == "fs.seek" =>
          fileSystem.file(handle.toInt) match {
            case None => throw new IOException("bad file descriptor")
            case Some(file) =>
              new String(whence, "UTF-8") match {
                case "cur" => file.seek(file.position + offset.toInt)
                case "set" => file.seek(offset.toLong)
                case "end" => file.seek(file.length + offset.toInt)
                case _ => throw new IllegalArgumentException("offset out of range")
              }
              result(file.position)
          }
        case Array(handle: Double, value: Array[Byte]) if message.name == "fs.write" =>
          fileSystem.file(handle.toInt) match {
            case None => throw new IOException("bad file descriptor")
            case Some(file) => file.write(value); result(true)
          }
        case _ => None
      }
    } catch {
      case e@(_: IOException | _: IllegalArgumentException) if e.getMessage != null && !e.getMessage.isEmpty =>
        result(Unit, e.getMessage)
      case e: FileNotFoundException =>
        result(Unit, "file not found")
      case e: SecurityException =>
        result(Unit, "access denied")
      case _: IllegalArgumentException =>
        result(Unit, "invalid argument")
      case e: IOException =>
        result(Unit, e.toString)
    }
  }

  private def clean(path: Array[Byte]) = {
    val result = com.google.common.io.Files.simplifyPath(new String(path, "UTF-8"))
    if (result.startsWith("../")) throw new FileNotFoundException()
    if (result == "/" || result == ".") ""
    else result
  }

  override protected def onDisconnect() {
    super.onDisconnect()
    fileSystem.close()
  }

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)

    val ownersNbt = nbt.getTagList("owners")
    (0 until ownersNbt.tagCount).map(ownersNbt.tagAt).map(_.asInstanceOf[NBTTagCompound]).foreach(ownerNbt => {
      val address = ownerNbt.getString("address")
      if (address != "") {
        val handlesNbt = ownerNbt.getTagList("handles")
        owners += address -> (0 until handlesNbt.tagCount).
          map(handlesNbt.tagAt).
          map(_.asInstanceOf[NBTTagInt].data).
          to[mutable.Set]
      }
    })
    if (nbt.hasKey("label"))
      label = nbt.getString("label")

    fileSystem.load(nbt.getCompoundTag("fs"))
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)

    val ownersNbt = new NBTTagList()
    for ((address, handles) <- owners) {
      val ownerNbt = new NBTTagCompound()
      ownerNbt.setString("address", address)
      val handlesNbt = new NBTTagList()
      for (handle <- handles) {
        handlesNbt.appendTag(new NBTTagInt(null, handle))
      }
      ownerNbt.setTag("handles", handlesNbt)
      ownersNbt.appendTag(ownerNbt)
    }
    nbt.setTag("owners", ownersNbt)
    if (label != "")
      nbt.setString("label", label)

    val fsNbt = new NBTTagCompound()
    fileSystem.save(fsNbt)
    nbt.setCompoundTag("fs", fsNbt)
  }
}