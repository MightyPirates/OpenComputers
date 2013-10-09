package li.cil.oc.server.fs

import java.io
import li.cil.oc.api.fs.Mode
import net.minecraft.nbt.{NBTTagList, NBTTagCompound}
import scala.collection.mutable

class VirtualFileSystem extends OutputStreamFileSystem {
  private val root = new VirtualDirectory

  // ----------------------------------------------------------------------- //

  override def exists(path: String) =
    root.exists(segments(path))

  override def isDirectory(path: String) =
    root.isDirectory(segments(path))

  override def size(path: String) =
    root.size(segments(path))

  override def list(path: String) =
    root.list(segments(path))

  override def rename(from: String, to: String) =
    if (from != "" && exists(from) && !exists(to)) {
      root.get(segments(to).dropRight(1)) match {
        case Some(toParent: VirtualDirectory) =>
          val fromParent = root.get(segments(from).dropRight(1)).get.asInstanceOf[VirtualDirectory]
          val fromName = segments(from).last
          val toName = segments(to).last
          val obj = fromParent.children(fromName)
          fromParent.children -= fromName
          toParent.children += toName -> obj
          true
        case _ => false
      }
    }
    else false

  // ----------------------------------------------------------------------- //

  override protected def makeDirectory(path: String) =
    root.makeDirectory(segments(path))

  override protected def delete(path: String) =
    root.delete(segments(path))

  // ----------------------------------------------------------------------- //

  override def close() = {
    super.close()
    root.children.clear()
  }

  // ----------------------------------------------------------------------- //

  override protected def openInputStream(path: String) =
    root.openInputStream(segments(path))

  override protected def openOutputStream(path: String, mode: Mode.Value) =
    root.openOutputStream(segments(path), mode)

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) = {
    root.load(nbt)
    super.load(nbt) // Last to ensure streams can be re-opened.
  }

  override def save(nbt: NBTTagCompound) = {
    super.save(nbt) // First to allow flushing.
    root.save(nbt)
  }

  // ----------------------------------------------------------------------- //

  private def segments(path: String) = path.split("/").view.filter(_ != "")

  // ----------------------------------------------------------------------- //

  private abstract class VirtualObject {
    def exists(path: Iterable[String]) = path.isEmpty

    def isDirectory(path: Iterable[String] = Iterable.empty[String]): Boolean

    def size(path: Iterable[String]): Long

    def list(path: Iterable[String]): Option[Array[String]]

    def makeDirectory(path: Iterable[String]): Boolean

    def delete(path: Iterable[String]): Boolean

    def canDelete: Boolean

    def openInputStream(path: Iterable[String]): Option[io.InputStream]

    def openOutputStream(path: Iterable[String], mode: Mode.Value): Option[io.OutputStream]

    def load(nbt: NBTTagCompound)

    def save(nbt: NBTTagCompound)

    def get(path: Iterable[String]): Option[VirtualObject] = if (path.isEmpty) Some(this) else None
  }

  private class VirtualFile extends VirtualObject {
    var data = Array.empty[Byte]

    var stream: Option[VirtualFileOutputStream] = None

    override def isDirectory(path: Iterable[String]) = false

    override def size(path: Iterable[String]) = data.length

    override def list(path: Iterable[String]) = None

    override def makeDirectory(path: Iterable[String]) = false

    override def delete(path: Iterable[String]) = false

    override def canDelete = stream.isEmpty

    override def openInputStream(path: Iterable[String]) =
      if (path.isEmpty) Some(new VirtualFileInputStream(this))
      else None

    override def openOutputStream(path: Iterable[String], mode: Mode.Value) =
      if (path.isEmpty) {
        if (stream.isDefined) throw new io.FileNotFoundException()
        if (mode == Mode.Write)
          data = Array.empty[Byte]
        stream = Some(new VirtualFileOutputStream(this))
        stream
      }
      else None

    override def load(nbt: NBTTagCompound) {
      data = nbt.getByteArray("data")
    }

    override def save(nbt: NBTTagCompound) {
      nbt.setByteArray("data", data)
    }
  }

  private class VirtualDirectory extends VirtualObject {
    val children = mutable.Map.empty[String, VirtualObject]

    override def exists(path: Iterable[String]) = super.exists(path) || (children.get(path.head) match {
      case Some(child) => child.exists(path.drop(1))
      case _ => false
    })

    override def isDirectory(path: Iterable[String]) = path.isEmpty || (children.get(path.head) match {
      case Some(child) => child.isDirectory(path.drop(1))
      case _ => false
    })

    override def size(path: Iterable[String]) =
      if (path.isEmpty) 0
      else children.get(path.head) match {
        case Some(child) => child.size(path.drop(1))
        case _ => 0
      }

    override def list(path: Iterable[String]) =
      if (path.isEmpty) Some(children.map {
        case (childName, child) => if (child.isDirectory()) childName + "/" else childName
      }.toArray)
      else children.get(path.head) match {
        case Some(child) => child.list(path.drop(1))
        case _ => None
      }

    override def makeDirectory(path: Iterable[String]) =
      if (path.size == 1) {
        val directory = path.head
        if (children.contains(directory)) false
        else {
          children += directory -> new VirtualDirectory
          true
        }
      } else children.get(path.head) match {
        case Some(child) => child.makeDirectory(path.drop(1))
        case _ => false
      }

    override def delete(path: Iterable[String]) =
      if (path.size == 1) {
        val childName = path.head
        children.get(childName) match {
          case Some(child) if child.canDelete =>
            children -= childName
            true
          case _ =>
            false
        }
      } else children.get(path.head) match {
        case Some(child) => child.delete(path.drop(1))
        case _ => false
      }

    override def canDelete = children.isEmpty

    override def openInputStream(path: Iterable[String]) =
      if (path.isEmpty) None
      else children.get(path.head) match {
        case Some(child) => child.openInputStream(path.drop(1))
        case _ => None
      }

    override def openOutputStream(path: Iterable[String], mode: Mode.Value) =
      if (path.isEmpty) None
      else children.get(path.head) match {
        case Some(child) => child.openOutputStream(path.drop(1), mode)
        case None if path.size == 1 =>
          val childName = path.head
          val child = new VirtualFile
          children += childName -> child
          child.openOutputStream(Array.empty[String], mode)
        case _ => None
      }

    override def load(nbt: NBTTagCompound) {
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
      val childrenNbt = new NBTTagList()
      for ((childName, child) <- children) {
        val childNbt = new NBTTagCompound()
        childNbt.setBoolean("isDirectory", child.isDirectory())
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
  }

  // ----------------------------------------------------------------------- //

  private class VirtualFileInputStream(val file: VirtualFile) extends io.InputStream {
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
          Array.copy(file.data, position, b, off, n)
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

  private class VirtualFileOutputStream(val file: VirtualFile) extends io.ByteArrayOutputStream {
    private var isClosed = false

    override def close() = if (!isClosed) {
      flush()
      isClosed = true
      file.stream = None
    }

    override def flush() =
      if (!isClosed) {
        file.data ++= toByteArray
        reset()
      } else throw new io.IOException("file is closed")

    override def write(b: Array[Byte], off: Int, len: Int) =
      if (!isClosed) super.write(b, off, len)
      else throw new io.IOException("file is closed")

    override def write(b: Int) =
      if (!isClosed) super.write(b)
      else throw new io.IOException("file is closed")
  }

}
