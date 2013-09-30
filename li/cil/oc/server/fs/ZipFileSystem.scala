package li.cil.oc.server.fs

import java.io.{IOException, InputStream, FileNotFoundException}
import java.util.zip.{ZipEntry, ZipFile}
import li.cil.oc.api
import li.cil.oc.api.fs.{File, Mode}
import net.minecraft.nbt.{NBTTagList, NBTTagCompound}
import scala.collection.mutable

class ZipFileSystem(val zip: ZipFile, val root: String) extends api.FileSystem {
  def exists(path: String) = entry(path).isDefined

  def size(path: String) = entry(path).fold(0L)(_.getSize)

  def isDirectory(path: String) = entry(path).exists(_.isDirectory)

  def list(path: String) = entry(path) match {
    case Some(entry) if entry.isDirectory => Some(entries(entry))
    case _ => None
  }

  def open(path: String, mode: Mode.Value) = if (mode == Mode.Read) {
    entry(path).filter(_.isDirectory) match {
      case Some(entry) => openHandle(entry)
      case _ => throw new FileNotFoundException(path)
    }
  } else throw new FileNotFoundException(path)

  def file(handle: Long) = Option(handles(handle): File)

  def load(nbt: NBTTagCompound) {
    val handlesNbt = nbt.getTagList("handles")
    (0 until handlesNbt.tagCount).map(handlesNbt.tagAt).map(_.asInstanceOf[NBTTagCompound]).foreach(handleNbt => {
      val handle = handleNbt.getLong("handle")
      val name = handleNbt.getString("name")
      val position = handleNbt.getLong("position")
      entry(name) match {
        case Some(entry) =>
          val file = new ZipFile(this, handle, name, zip.getInputStream(entry))
          val skipped = file.stream.skip(position)
          file.position = skipped // May be != position if the file changed since we saved.
          handles += handle -> file
        case _ => // The source file seems to have changed since last time.
      }
    })
  }

  def save(nbt: NBTTagCompound) {
    val handlesNbt = new NBTTagList()
    for (file <- handles.values) {
      assert(!file.isClosed)
      val handleNbt = new NBTTagCompound()
      handleNbt.setLong("handle", file.handle)
      handleNbt.setString("name", file.name)
      handleNbt.setLong("position", file.position)
      handlesNbt.appendTag(handleNbt)
    }
    nbt.setTag("handles", handlesNbt)
  }

  // ----------------------------------------------------------------------- //

  private val directories = mutable.Map.empty[ZipEntry, Array[String]]

  private def entry(path: String) = Option(zip.getEntry(root + path))

  private def entries(path: ZipEntry) = directories.get(path).getOrElse {
    val pathName = root + path.getName
    val list = mutable.ArrayBuffer.empty[String]
    val iterator = zip.entries
    while (iterator.hasMoreElements) {
      val child = iterator.nextElement
      if (child.getName.startsWith(pathName)) {
        list += child.getName.substring(pathName.length)
      }
    }
    val children = list.toArray
    directories += path -> children
    children
  }

  // ----------------------------------------------------------------------- //

  private class ZipFile(val owner: ZipFileSystem, val handle: Long, val name: String, val stream: InputStream) extends File {
    var isClosed = false
    var position = 0L

    def close() = if (!isClosed) {
      isClosed = true
      owner.handles -= handle
      stream.close()
    }

    def read(into: Array[Byte]) = {
      val read = stream.read(into)
      if (read >= 0)
        position += read
      read
    }

    def write(value: Array[Byte]) = throw new IOException()
  }

  private val handles = mutable.Map.empty[Long, ZipFile]

  private val maxHandles = 32

  private def openHandle(entry: ZipEntry) = if (handles.size < maxHandles) {
    val handle = Iterator.continually((Math.random() * Long.MaxValue).toLong + 1).filterNot(handles.contains).next()
    handles += handle -> new ZipFile(this, handle, entry.getName, zip.getInputStream(entry))
    handle
  } else 0L
}
