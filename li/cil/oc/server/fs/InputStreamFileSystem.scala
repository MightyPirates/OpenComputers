package li.cil.oc.server.fs

import java.io.{FileNotFoundException, IOException, InputStream}
import li.cil.oc.api
import li.cil.oc.api.fs.Mode
import net.minecraft.nbt.{NBTTagList, NBTTagCompound}
import scala.collection.mutable

abstract class InputStreamFileSystem extends api.FileSystem {
  private val handles = mutable.Map.empty[Int, Handle]

  protected def maxHandles = 32

  def open(path: String, mode: Mode.Value) = if (mode == Mode.Read && exists(path) && !isDirectory(path)) {
    if (handles.size >= maxHandles) throw new IOException("too many open files")
    val handle = Iterator.continually((Math.random() * Int.MaxValue).toInt + 1).filterNot(handles.contains).next()
    openInputStream(path, handle) match {
      case Some(stream) =>
        handles += handle -> new Handle(this, handle, path, stream)
        handle
      case _ => throw new FileNotFoundException(path)
    }
  } else throw new FileNotFoundException(path)

  def file(handle: Int) = handles.get(handle): Option[api.fs.File]

  def close() {
    for (handle <- handles.values)
      handle.close()
  }

  def load(nbt: NBTTagCompound) {
    val handlesNbt = nbt.getTagList("handles")
    (0 until handlesNbt.tagCount).map(handlesNbt.tagAt).map(_.asInstanceOf[NBTTagCompound]).foreach(handleNbt => {
      val handle = handleNbt.getInteger("handle")
      val path = handleNbt.getString("path")
      val position = handleNbt.getLong("position")
      openInputStream(path, handle) match {
        case Some(stream) =>
          val inputHandle = new Handle(this, handle, path, stream)
          inputHandle.position = stream.skip(position) // May be != position if the file changed since we saved.
          handles += handle -> inputHandle
        case _ => // The source file seems to have changed since last time.
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
    nbt.setTag("handles", handlesNbt)
  }

  protected def openInputStream(path: String, handle: Long): Option[InputStream]

  private class Handle(val owner: InputStreamFileSystem, val handle: Int, val path: String, val stream: InputStream) extends api.fs.File {
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

    def write(value: Array[Byte]) = throw new IOException()
  }

}
