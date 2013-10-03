package li.cil.oc.server.fs

import java.io.{FileNotFoundException, IOException, InputStream}
import li.cil.oc.api
import li.cil.oc.api.fs.Mode
import net.minecraft.nbt.{NBTTagList, NBTTagCompound}
import scala.collection.mutable

abstract class InputStreamFileSystem extends api.FileSystem {
  private val handles = mutable.Map.empty[Int, Handle]

  def open(path: String, mode: Mode.Value) = if (mode == Mode.Read && exists(path) && !isDirectory(path)) {
    val handle = Iterator.continually((Math.random() * Int.MaxValue).toInt + 1).filterNot(handles.contains).next()
    openInputStream(path) match {
      case Some(stream) =>
        handles += handle -> new Handle(this, handle, path, stream)
        handle
      case _ => throw new FileNotFoundException()
    }
  } else throw new FileNotFoundException()

  def file(handle: Int) = handles.get(handle): Option[api.fs.Handle]

  def close() {
    for (handle <- handles.values)
      handle.close()
    handles.clear()
  }

  def load(nbt: NBTTagCompound) {
    val handlesNbt = nbt.getTagList("input")
    (0 until handlesNbt.tagCount).map(handlesNbt.tagAt).map(_.asInstanceOf[NBTTagCompound]).foreach(handleNbt => {
      val handle = handleNbt.getInteger("handle")
      val path = handleNbt.getString("path")
      val position = handleNbt.getLong("position")
      openInputStream(path) match {
        case Some(stream) =>
          val fileHandle = new Handle(this, handle, path, stream)
          fileHandle.position = stream.skip(position) // May be != position if the file changed since we saved.
          handles += handle -> fileHandle
        case _ => // The source file seems to have disappeared since last time.
      }
    })
  }

  def save(nbt: NBTTagCompound) {
    val handlesNbt = new NBTTagList()
    for (file <- handles.values) {
      assert(!file.isClosed)
      val handleNbt = new NBTTagCompound()
      handleNbt.setInteger("handle", file.handle)
      handleNbt.setString("path", file.path)
      handleNbt.setLong("position", file.position)
      handlesNbt.appendTag(handleNbt)
    }
    nbt.setTag("input", handlesNbt)
  }

  protected def openInputStream(path: String): Option[InputStream]

  private class Handle(val owner: InputStreamFileSystem, val handle: Int, val path: String, val stream: InputStream) extends api.fs.Handle {
    var isClosed = false
    var position = 0L

    def length = owner.size(path)

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

    def seek(to: Long) = {
      stream.reset()
      stream.skip(to)
    }

    def write(value: Array[Byte]) = throw new IOException("handle is read-only")
  }

}
