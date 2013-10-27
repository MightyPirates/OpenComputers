package li.cil.oc.server.component

import java.io.{FileNotFoundException, IOException}
import li.cil.oc.api.fs.Mode
import li.cil.oc.api.network.environment.LuaCallback
import li.cil.oc.api.network.{Visibility, Message}
import li.cil.oc.{Config, api}
import net.minecraft.nbt.{NBTTagInt, NBTTagList, NBTTagCompound}
import scala.collection.mutable

class FileSystem(val fileSystem: api.fs.FileSystem) extends ManagedComponent {
  val node = api.Network.createComponent(api.Network.createNode(this, "filesystem", Visibility.Neighbors))

  private val owners = mutable.Map.empty[String, mutable.Set[Int]]

  private var label = ""

  // ----------------------------------------------------------------------- //

  @LuaCallback("getLabel")
  def getLabel(message: Message): Array[Object] = Array(label)

  @LuaCallback("setLabel")
  def setLabel(message: Message): Array[Object] = {
    label = message.checkString(1)
    if (label.length > 16)
      label = label.substring(0, 16)
    result(true)
  }

  @LuaCallback("spaceTotal")
  def spaceTotal(message: Message): Array[Object] = {
    val space = fileSystem.spaceTotal
    if (space < 0)
      Array("unlimited")
    else
      result(space)
  }

  @LuaCallback("spaceUsed")
  def spaceUsed(message: Message): Array[Object] =
    result(fileSystem.spaceUsed)

  @LuaCallback("exists")
  def exists(message: Message): Array[Object] =
    result(fileSystem.exists(clean(message.checkString(1))))

  @LuaCallback("size")
  def size(message: Message): Array[Object] =
    result(fileSystem.size(clean(message.checkString(1))))

  @LuaCallback("isDirectory")
  def isDirectory(message: Message): Array[Object] =
    result(fileSystem.isDirectory(clean(message.checkString(1))))

  @LuaCallback("lastModified")
  def lastModified(message: Message): Array[Object] =
    result(fileSystem.lastModified(clean(message.checkString(1))))

  @LuaCallback("list")
  def list(message: Message): Array[Object] =
    Option(fileSystem.list(clean(message.checkString(1)))) match {
      case Some(list) => Array(list)
      case _ => null
    }

  @LuaCallback("makeDirectory")
  def makeDirectory(message: Message): Array[Object] = {
    def recurse(path: String): Boolean = !fileSystem.exists(path) && (fileSystem.makeDirectory(path) ||
      (recurse(path.split("/").dropRight(1).mkString("/")) && fileSystem.makeDirectory(path)))
    result(recurse(clean(message.checkString(1))))
  }

  @LuaCallback("remove")
  def remove(message: Message): Array[Object] = {
    def recurse(parent: String): Boolean = (!fileSystem.isDirectory(parent) ||
      fileSystem.list(parent).forall(child => recurse(parent + "/" + child))) && fileSystem.delete(parent)
    result(recurse(clean(message.checkString(1))))
  }

  @LuaCallback("rename")
  def rename(message: Message): Array[Object] =
    result(fileSystem.rename(clean(message.checkString(1)), clean(message.checkString(2))))

  @LuaCallback("close")
  def close(message: Message): Array[Object] = {
    val handle = message.checkInteger(1)
    Option(fileSystem.file(handle)) match {
      case Some(file) =>
        owners.get(message.source.address) match {
          case Some(set) => if (set.remove(handle)) file.close()
          case _ => // Not the owner of this handle.
        }
      case _ => Array(Unit, "bad file descriptor")
    }
    null
  }

  @LuaCallback("open")
  def open(message: Message): Array[Object] =
    if (owners.get(message.source.address).fold(false)(_.size >= Config.maxHandles))
      result(Unit, "too many open handles")
    else {
      val path = message.checkString(1)
      val mode = if (message.data.length > 2) message.checkString(2) else "r"
      val handle = fileSystem.open(clean(path), Mode.parse(mode))
      if (handle > 0) {
        owners.getOrElseUpdate(message.source.address, mutable.Set.empty[Int]) += handle
      }
      result(handle)
    }

  @LuaCallback("read")
  def read(message: Message): Array[Object] = {
    val handle = message.checkInteger(1)
    val n = message.checkInteger(2)
    Option(fileSystem.file(handle)) match {
      case None => throw new IOException("bad file descriptor")
      case Some(file) =>
        // Limit reading to chunks of 8KB to avoid crazy allocations.
        val buffer = new Array[Byte](n min (8 * 1024))
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
  }

  @LuaCallback("seek")
  def seek(message: Message): Array[Object] = {
    val handle = message.checkInteger(1)
    val whence = message.checkString(2)
    val offset = message.checkInteger(3)
    Option(fileSystem.file(handle)) match {
      case Some(file) =>
        whence match {
          case "cur" => file.seek(file.position + offset)
          case "set" => file.seek(offset.toLong)
          case "end" => file.seek(file.length + offset)
          case _ => throw new IllegalArgumentException("invalid mode")
        }
        result(file.position)
      case _ => throw new IOException("bad file descriptor")
    }
  }

  @LuaCallback("write")
  def write(message: Message): Array[Object] = {
    val handle = message.checkInteger(1)
    val value = message.checkByteArray(2)
    Option(fileSystem.file(handle)) match {
      case Some(file) => file.write(value); result(true)
      case _ => throw new IOException("bad file descriptor")
    }
  }

  // ----------------------------------------------------------------------- //

  override def onMessage(message: Message) = {
    message.data match {
      case Array() if message.name == "system.disconnect" && owners.contains(message.source.address) =>
        for (handle <- owners(message.source.address)) {
          Option(fileSystem.file(handle)) match {
            case Some(file) => file.close()
            case _ => // Maybe file system was accessed from somewhere else.
          }
        }
      case Array() if message.name == "computer.stopped" =>
        owners.get(message.source.address) match {
          case Some(set) =>
            set.foreach(handle => Option(fileSystem.file(handle)) match {
              case Some(file) => file.close()
              case _ => // Invalid handle... huh.
            })
            set.clear()
          case _ => // Computer had no open files.
        }
      case _ =>
    }
    null
  }

  override def onDisconnect() {
    fileSystem.close()
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
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

  // ----------------------------------------------------------------------- //

  private def clean(path: String) = {
    val result = com.google.common.io.Files.simplifyPath(path)
    if (result.startsWith("../")) throw new FileNotFoundException()
    if (result == "/" || result == ".") ""
    else result
  }
}