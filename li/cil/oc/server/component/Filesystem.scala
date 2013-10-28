package li.cil.oc.server.component

import java.io.{FileNotFoundException, IOException}
import li.cil.oc.api.fs.Mode
import li.cil.oc.api.network.environment.{Arguments, Context, LuaCallback}
import li.cil.oc.api.network.{Visibility, Message}
import li.cil.oc.server.fs.Volatile
import li.cil.oc.{Config, api}
import net.minecraft.nbt.{NBTTagInt, NBTTagList, NBTTagCompound}
import scala.collection.mutable

class FileSystem(val fileSystem: api.fs.FileSystem, var label: String) extends ManagedComponent {
  val node = api.Network.createComponent(api.Network.createNode(this, "filesystem", Visibility.Neighbors))

  private val owners = mutable.Map.empty[String, mutable.Set[Int]]

  // ----------------------------------------------------------------------- //

  @LuaCallback("getLabel")
  def getLabel(context: Context, args: Arguments): Array[Object] = Array(label)

  @LuaCallback("setLabel")
  def setLabel(context: Context, args: Arguments): Array[Object] = {
    if (fileSystem.isReadOnly)
      throw new IllegalArgumentException("file system is read only")
    if (fileSystem.isInstanceOf[Volatile])
      throw new IllegalArgumentException("cannot change label of ramfs")
    if (args.checkAny(1) == null)
      label = null
    else {
      label = args.checkString(1)
      if (label.length > 16)
        label = label.substring(0, 16)
    }
    result(true)
  }

  @LuaCallback(value = "isReadOnly", asynchronous = true)
  def isReadOnly(context: Context, args: Arguments): Array[Object] =
    result(fileSystem.isReadOnly)

  @LuaCallback(value = "spaceTotal", asynchronous = true)
  def spaceTotal(context: Context, args: Arguments): Array[Object] = {
    val space = fileSystem.spaceTotal
    if (space < 0)
      Array("unlimited")
    else
      result(space)
  }

  @LuaCallback("spaceUsed")
  def spaceUsed(context: Context, args: Arguments): Array[Object] =
    result(fileSystem.spaceUsed)

  @LuaCallback("exists")
  def exists(context: Context, args: Arguments): Array[Object] =
    result(fileSystem.exists(clean(args.checkString(1))))

  @LuaCallback("size")
  def size(context: Context, args: Arguments): Array[Object] =
    result(fileSystem.size(clean(args.checkString(1))))

  @LuaCallback("isDirectory")
  def isDirectory(context: Context, args: Arguments): Array[Object] =
    result(fileSystem.isDirectory(clean(args.checkString(1))))

  @LuaCallback("lastModified")
  def lastModified(context: Context, args: Arguments): Array[Object] =
    result(fileSystem.lastModified(clean(args.checkString(1))))

  @LuaCallback("list")
  def list(context: Context, args: Arguments): Array[Object] =
    Option(fileSystem.list(clean(args.checkString(1)))) match {
      case Some(list) => Array(list)
      case _ => null
    }

  @LuaCallback("makeDirectory")
  def makeDirectory(context: Context, args: Arguments): Array[Object] = {
    def recurse(path: String): Boolean = !fileSystem.exists(path) && (fileSystem.makeDirectory(path) ||
      (recurse(path.split("/").dropRight(1).mkString("/")) && fileSystem.makeDirectory(path)))
    result(recurse(clean(args.checkString(1))))
  }

  @LuaCallback("remove")
  def remove(context: Context, args: Arguments): Array[Object] = {
    def recurse(parent: String): Boolean = (!fileSystem.isDirectory(parent) ||
      fileSystem.list(parent).forall(child => recurse(parent + "/" + child))) && fileSystem.delete(parent)
    result(recurse(clean(args.checkString(1))))
  }

  @LuaCallback("rename")
  def rename(context: Context, args: Arguments): Array[Object] =
    result(fileSystem.rename(clean(args.checkString(1)), clean(args.checkString(2))))

  @LuaCallback("close")
  def close(context: Context, args: Arguments): Array[Object] = {
    val handle = args.checkInteger(1)
    Option(fileSystem.getHandle(handle)) match {
      case Some(file) =>
        owners.get(context.address) match {
          case Some(set) => if (set.remove(handle)) file.close()
          case _ => // Not the owner of this handle.
        }
      case _ => Array(Unit, "bad file descriptor")
    }
    null
  }

  @LuaCallback("open")
  def open(context: Context, args: Arguments): Array[Object] =
    if (owners.get(context.address).fold(false)(_.size >= Config.maxHandles))
      result(Unit, "too many open handles")
    else {
      val path = args.checkString(1)
      val mode = if (args.count > 1) args.checkString(2) else "r"
      val handle = fileSystem.open(clean(path), parseMode(mode))
      if (handle > 0) {
        owners.getOrElseUpdate(context.address, mutable.Set.empty[Int]) += handle
      }
      result(handle)
    }

  @LuaCallback("read")
  def read(context: Context, args: Arguments): Array[Object] = {
    val handle = args.checkInteger(1)
    val n = args.checkInteger(2)
    Option(fileSystem.getHandle(handle)) match {
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
  def seek(context: Context, args: Arguments): Array[Object] = {
    val handle = args.checkInteger(1)
    val whence = args.checkString(2)
    val offset = args.checkInteger(3)
    Option(fileSystem.getHandle(handle)) match {
      case Some(file) =>
        whence match {
          case "cur" => file.seek(file.position + offset)
          case "set" => file.seek(offset)
          case "end" => file.seek(file.length + offset)
          case _ => throw new IllegalArgumentException("invalid mode")
        }
        result(file.position)
      case _ => throw new IOException("bad file descriptor")
    }
  }

  @LuaCallback("write")
  def write(context: Context, args: Arguments): Array[Object] = {
    val handle = args.checkInteger(1)
    val value = args.checkByteArray(2)
    Option(fileSystem.getHandle(handle)) match {
      case Some(file) => file.write(value); result(true)
      case _ => throw new IOException("bad file descriptor")
    }
  }

  // ----------------------------------------------------------------------- //

  override def onMessage(message: Message) = {
    message.data match {
      case Array() if message.name == "system.disconnect" && owners.contains(message.source.address) =>
        for (handle <- owners(message.source.address)) {
          Option(fileSystem.getHandle(handle)) match {
            case Some(file) => file.close()
            case _ => // Maybe file system was accessed from somewhere else.
          }
        }
      case Array() if message.name == "computer.stopped" =>
        owners.get(message.source.address) match {
          case Some(set) =>
            set.foreach(handle => Option(fileSystem.getHandle(handle)) match {
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
    if (label != null)
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

  private def parseMode(value: String): Mode = {
    if (("r" == value) || ("rb" == value)) return Mode.Read
    if (("w" == value) || ("wb" == value)) return Mode.Write
    if (("a" == value) || ("ab" == value)) return Mode.Append
    throw new IllegalArgumentException("unsupported mode")
  }
}