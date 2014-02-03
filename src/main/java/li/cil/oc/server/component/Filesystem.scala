package li.cil.oc.server.component

import java.io.{FileNotFoundException, IOException}
import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.fs.{Label, Mode, FileSystem => IFileSystem}
import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.{NBTTagInt, NBTTagList, NBTTagCompound}
import scala.Some
import scala.collection.mutable

class FileSystem(val fileSystem: IFileSystem, var label: Label) extends ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("filesystem", Visibility.Neighbors).
    withConnector().
    create()

  private val owners = mutable.Map.empty[String, mutable.Set[Int]]

  // ----------------------------------------------------------------------- //

  @Callback(direct = true)
  def getLabel(context: Context, args: Arguments): Array[AnyRef] = result(label.getLabel)

  @Callback
  def setLabel(context: Context, args: Arguments): Array[AnyRef] = {
    if (label == null) throw new Exception("filesystem does not support labeling")
    if (args.checkAny(0) == null) label.setLabel(null)
    else label.setLabel(args.checkString(0))
    result(label.getLabel)
  }

  @Callback(direct = true)
  def isReadOnly(context: Context, args: Arguments): Array[AnyRef] = {
    result(fileSystem.isReadOnly)
  }

  @Callback(direct = true)
  def spaceTotal(context: Context, args: Arguments): Array[AnyRef] = {
    val space = fileSystem.spaceTotal
    if (space < 0)
      Array("unlimited")
    else
      result(space)
  }

  @Callback(direct = true)
  def spaceUsed(context: Context, args: Arguments): Array[AnyRef] = {
    result(fileSystem.spaceUsed)
  }

  @Callback(direct = true)
  def exists(context: Context, args: Arguments): Array[AnyRef] = {
    result(fileSystem.exists(clean(args.checkString(0))))
  }

  @Callback(direct = true)
  def size(context: Context, args: Arguments): Array[AnyRef] = {
    result(fileSystem.size(clean(args.checkString(0))))
  }

  @Callback(direct = true)
  def isDirectory(context: Context, args: Arguments): Array[AnyRef] = {
    result(fileSystem.isDirectory(clean(args.checkString(0))))
  }

  @Callback(direct = true)
  def lastModified(context: Context, args: Arguments): Array[AnyRef] = {
    result(fileSystem.lastModified(clean(args.checkString(0))))
  }

  @Callback
  def list(context: Context, args: Arguments): Array[AnyRef] = {
    Option(fileSystem.list(clean(args.checkString(0)))) match {
      case Some(list) => Array(list)
      case _ => null
    }
  }

  @Callback
  def makeDirectory(context: Context, args: Arguments): Array[AnyRef] = {
    def recurse(path: String): Boolean = !fileSystem.exists(path) && (fileSystem.makeDirectory(path) ||
      (recurse(path.split("/").dropRight(1).mkString("/")) && fileSystem.makeDirectory(path)))
    result(recurse(clean(args.checkString(0))))
  }

  @Callback
  def remove(context: Context, args: Arguments): Array[AnyRef] = {
    def recurse(parent: String): Boolean = (!fileSystem.isDirectory(parent) ||
      fileSystem.list(parent).forall(child => recurse(parent + "/" + child))) && fileSystem.delete(parent)
    result(recurse(clean(args.checkString(0))))
  }

  @Callback
  def rename(context: Context, args: Arguments): Array[AnyRef] = {
    result(fileSystem.rename(clean(args.checkString(0)), clean(args.checkString(1))))
  }

  @Callback
  def close(context: Context, args: Arguments): Array[AnyRef] = {
    val handle = args.checkInteger(0)
    Option(fileSystem.getHandle(handle)) match {
      case Some(file) =>
        owners.get(context.address) match {
          case Some(set) if set.remove(handle) => file.close()
          case _ => throw new IOException("bad file descriptor")
        }
      case _ => throw new IOException("bad file descriptor")
    }
    null
  }

  @Callback
  def open(context: Context, args: Arguments): Array[AnyRef] = {
    if (owners.get(context.address).fold(false)(_.size >= Settings.get.maxHandles)) {
      throw new IOException("too many open handles")
    }
    val path = args.checkString(0)
    val mode = if (args.count > 1) args.checkString(1) else "r"
    val handle = fileSystem.open(clean(path), parseMode(mode))
    if (handle > 0) {
      owners.getOrElseUpdate(context.address, mutable.Set.empty[Int]) += handle
    }
    result(handle)
  }

  @Callback
  def read(context: Context, args: Arguments): Array[AnyRef] = {
    val handle = args.checkInteger(0)
    val n = math.min(Settings.get.maxReadBuffer, math.max(0, args.checkInteger(1)))
    checkOwner(context.address, handle)
    Option(fileSystem.getHandle(handle)) match {
      case Some(file) =>
        // Limit size of read buffer to avoid crazy allocations.
        val buffer = new Array[Byte](n)
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
          if (!node.tryChangeBuffer(-Settings.get.hddReadCost * bytes.length)) {
            throw new IOException("not enough energy")
          }
          result(bytes)
        }
        else {
          result(Unit)
        }
      case _ => throw new IOException("bad file descriptor")
    }
  }

  @Callback
  def seek(context: Context, args: Arguments): Array[AnyRef] = {
    val handle = args.checkInteger(0)
    val whence = args.checkString(1)
    val offset = args.checkInteger(2)
    checkOwner(context.address, handle)
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

  @Callback
  def write(context: Context, args: Arguments): Array[AnyRef] = {
    val handle = args.checkInteger(0)
    val value = args.checkByteArray(1)
    if (!node.tryChangeBuffer(-Settings.get.hddWriteCost * value.length)) {
      throw new IOException("not enough energy")
    }
    checkOwner(context.address, handle)
    Option(fileSystem.getHandle(handle)) match {
      case Some(file) => file.write(value); result(true)
      case _ => throw new IOException("bad file descriptor")
    }
  }

  // ----------------------------------------------------------------------- //

  override def onMessage(message: Message) {
    super.onMessage(message)
    message.data match {
      case Array() if message.name == "computer.stopped" || message.name == "computer.started" =>
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
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      fileSystem.close()
    }
    else if (owners.contains(node.address)) {
      for (handle <- owners(node.address)) {
        Option(fileSystem.getHandle(handle)) match {
          case Some(file) => file.close()
          case _ =>
        }
      }
      owners.remove(node.address)
    }
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

    nbt.setNewCompoundTag("fs", fileSystem.save)
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

  private def checkOwner(owner: String, handle: Int) =
    if (!owners.contains(owner) || !owners(owner).contains(handle))
      throw new IOException("bad file descriptor")
}