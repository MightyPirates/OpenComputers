package li.cil.oc.server.fs

import java.io
import li.cil.oc.api.fs.Mode
import net.minecraft.nbt.{NBTTagList, NBTTagCompound}
import scala.collection.mutable

trait VirtualFileSystem extends OutputStreamFileSystem {
  protected val root = new VirtualDirectory

  // ----------------------------------------------------------------------- //

  override def exists(path: String) =
    root.get(segments(path)).isDefined

  override def isDirectory(path: String) =
    root.get(segments(path)) match {
      case Some(obj) => obj.isDirectory
      case _ => false
    }

  override def size(path: String) =
    root.get(segments(path)) match {
      case Some(obj) => obj.size
      case _ => 0L
    }

  override def lastModified(path: String) =
    root.get(segments(path)) match {
      case Some(obj) => obj.lastModified
      case _ => 0L
    }

  override def list(path: String) =
    root.get(segments(path)) match {
      case Some(obj: VirtualDirectory) => obj.list()
      case _ => null
    }

  // ----------------------------------------------------------------------- //

  override def delete(path: String) = {
    val parts = segments(path)
    if (parts.isEmpty) false
    else {
      root.get(parts.dropRight(1)) match {
        case Some(parent: VirtualDirectory) => parent.delete(parts.last)
        case _ => false
      }
    }
  }

  override def makeDirectory(path: String) = {
    val parts = segments(path)
    if (parts.isEmpty) false
    else {
      root.get(parts.dropRight(1)) match {
        case Some(parent: VirtualDirectory) => parent.makeDirectory(parts.last)
        case _ => false
      }
    }
  }

  override def rename(from: String, to: String) =
    if (from != "" && exists(from) && !exists(to)) {
      val segmentsTo = segments(to)
      root.get(segmentsTo.dropRight(1)) match {
        case Some(toParent: VirtualDirectory) =>
          val toName = segmentsTo.last
          val segmentsFrom = segments(from)
          val fromParent = root.get(segmentsFrom.dropRight(1)).get.asInstanceOf[VirtualDirectory]
          val fromName = segmentsFrom.last
          val obj = fromParent.children(fromName)

          fromParent.children -= fromName
          fromParent.lastModified = System.currentTimeMillis()

          toParent.children += toName -> obj
          toParent.lastModified = System.currentTimeMillis()

          obj.lastModified = System.currentTimeMillis()
          true
        case _ => false
      }
    }
    else false

  override def setLastModified(path: String, time: Long) =
    root.get(segments(path)) match {
      case Some(obj) if time >= 0 =>
        obj.lastModified = time
        true
      case _ => false
    }

  // ----------------------------------------------------------------------- //

  override protected def openInputStream(path: String) =
    root.get(segments(path)) match {
      case Some(obj: VirtualFile) => obj.openInputStream()
      case _ => None
    }

  override protected def openOutputStream(path: String, mode: Mode) = {
    val parts = segments(path)
    if (parts.isEmpty) None
    else {
      root.get(parts.dropRight(1)) match {
        case Some(parent: VirtualDirectory) => parent.openOutputStream(parts.last, mode)
        case _ => None
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) = {
    root.load(nbt)
    super.readFromNBT(nbt) // Last to ensure streams can be re-opened.
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt) // First to allow flushing.
    root.save(nbt)
  }

  // ----------------------------------------------------------------------- //

  protected def segments(path: String) = path.split("/").view.filter(_ != "")

  // ----------------------------------------------------------------------- //

  protected abstract class VirtualObject {
    def isDirectory: Boolean

    def size: Long

    var lastModified = System.currentTimeMillis()

    def load(nbt: NBTTagCompound) {
      if (nbt.hasKey("lastModified"))
        lastModified = nbt.getLong("lastModified")
    }

    def save(nbt: NBTTagCompound) {
      nbt.setLong("lastModified", lastModified)
    }

    def get(path: Iterable[String]): Option[VirtualObject] =
      if (path.isEmpty) Some(this) else None

    def canDelete: Boolean
  }

  // ----------------------------------------------------------------------- //

  protected class VirtualFile extends VirtualObject {
    val data = mutable.ArrayBuffer.empty[Byte]

    var stream: Option[VirtualFileOutputStream] = None

    override def isDirectory = false

    override def size = data.length

    def openInputStream() = Some(new VirtualFileInputStream(this))

    def openOutputStream(mode: Mode) =
      if (stream.isDefined) None
      else {
        if (mode == Mode.Write) {
          data.clear()
          lastModified = System.currentTimeMillis()
        }
        stream = Some(new VirtualFileOutputStream(this))
        stream
      }

    override def load(nbt: NBTTagCompound) {
      super.load(nbt)
      data.clear()
      data ++= nbt.getByteArray("data")
    }

    override def save(nbt: NBTTagCompound) {
      super.save(nbt)
      nbt.setByteArray("data", data.toArray)
    }

    override def canDelete = stream.isEmpty
  }

  // ----------------------------------------------------------------------- //

  protected class VirtualDirectory extends VirtualObject {
    val children = mutable.Map.empty[String, VirtualObject]

    override def isDirectory = true

    override def size = 0

    def list() = children.map {
      case (childName, child) => if (child.isDirectory) childName + "/" else childName
    }.toArray

    def makeDirectory(name: String) =
      if (children.contains(name)) false
      else {
        children += name -> new VirtualDirectory
        lastModified = System.currentTimeMillis()
        true
      }

    def delete(name: String) = {
      children.get(name) match {
        case Some(child) if child.canDelete =>
          children -= name
          lastModified = System.currentTimeMillis()
          true
        case _ => false
      }
    }

    def openOutputStream(name: String, mode: Mode) =
      children.get(name) match {
        case Some(obj: VirtualFile) => obj.openOutputStream(mode)
        case None =>
          val child = new VirtualFile
          children += name -> child
          lastModified = System.currentTimeMillis()
          child.openOutputStream(mode)
        case _ => None
      }

    override def load(nbt: NBTTagCompound) {
      super.load(nbt)
      val childrenNbt = nbt.getTagList("children")
      (0 until childrenNbt.tagCount).map(childrenNbt.tagAt).map(_.asInstanceOf[NBTTagCompound]).foreach(childNbt => {
        val child =
          if (childNbt.getBoolean("isDirectory")) new VirtualDirectory
          else new VirtualFile
        child.load(childNbt)
        children += childNbt.getString("name") -> child
      })
    }

    override def save(nbt: NBTTagCompound) {
      super.save(nbt)
      val childrenNbt = new NBTTagList()
      for ((childName, child) <- children) {
        val childNbt = new NBTTagCompound()
        childNbt.setBoolean("isDirectory", child.isDirectory)
        childNbt.setString("name", childName)
        child.save(childNbt)
        childrenNbt.appendTag(childNbt)
      }
      nbt.setTag("children", childrenNbt)
    }

    override def get(path: Iterable[String]) =
      super.get(path) orElse {
        children.get(path.head) match {
          case Some(child) => child.get(path.drop(1))
          case _ => None
        }
      }

    override def canDelete = children.isEmpty
  }

  // ----------------------------------------------------------------------- //

  protected class VirtualFileInputStream(val file: VirtualFile) extends io.InputStream {
    private var isClosed = false

    private var position = 0

    override def available() =
      if (isClosed) 0
      else (file.data.length - position) max 0

    override def close() = isClosed = true

    def read() =
      if (!isClosed) {
        if (available == 0) -1
        else {
          position += 1
          file.data(position - 1)
        }
      }
      else throw new io.IOException("file is closed")

    override def read(b: Array[Byte], off: Int, len: Int) =
      if (!isClosed) {
        if (available == 0) -1
        else {
          val n = len min available
          file.data.view(position, file.data.length).copyToArray(b, off, n)
          position += n
          n
        }
      }
      else throw new io.IOException("file is closed")

    override def reset() =
      if (!isClosed) {
        position = 0
      }
      else throw new io.IOException("file is closed")

    override def skip(n: Long) =
      if (!isClosed) {
        position = ((position + n) min Int.MaxValue).toInt
        position
      }
      else throw new io.IOException("file is closed")
  }

  // ----------------------------------------------------------------------- //

  protected class VirtualFileOutputStream(val file: VirtualFile) extends io.OutputStream {
    private var isClosed = false

    override def close() = if (!isClosed) {
      isClosed = true
      file.stream = None
    }

    override def write(b: Array[Byte], off: Int, len: Int) =
      if (!isClosed) {
        file.data ++= b.view(off, off + len)
        file.lastModified = System.currentTimeMillis()
      }
      else throw new io.IOException("file is closed")

    override def write(b: Int) =
      if (!isClosed) {
        file.data += b.toByte
        file.lastModified = System.currentTimeMillis()
      }
      else throw new io.IOException("file is closed")
  }

}
