package li.cil.oc.server.fs

import java.io.{FileNotFoundException, IOException, InputStream}
import li.cil.oc.api
import li.cil.oc.api.fs.Mode
import net.minecraft.nbt.{NBTTagList, NBTTagCompound}
import scala.collection.mutable

trait InputStreamFileSystem extends api.fs.FileSystem {
  private val handles = mutable.Map.empty[Int, Handle]

  // ----------------------------------------------------------------------- //

  override def isReadOnly = true

  override def delete(path: String) = false

  override def makeDirectory(path: String) = false

  override def rename(from: String, to: String) = false

  override def setLastModified(path: String, time: Long) = false

  // ----------------------------------------------------------------------- //

  override def open(path: String, mode: Mode) = if (mode == Mode.Read && exists(path) && !isDirectory(path)) {
    val handle = Iterator.continually((Math.random() * Int.MaxValue).toInt + 1).filterNot(handles.contains).next()
    openInputStream(path) match {
      case Some(stream) =>
        handles += handle -> new Handle(this, handle, path, stream)
        handle
      case _ => throw new FileNotFoundException()
    }
  } else throw new FileNotFoundException()

  override def getHandle(handle: Int): api.fs.Handle = handles.get(handle).orNull

  override def close() {
    for (handle <- handles.values)
      handle.close()
    handles.clear()
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
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

  override def save(nbt: NBTTagCompound) {
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

  // ----------------------------------------------------------------------- //

  protected def openInputStream(path: String): Option[InputStream]

  // ----------------------------------------------------------------------- //

  private class Handle(val owner: InputStreamFileSystem, val handle: Int, val path: String, val stream: InputStream) extends api.fs.Handle {
    var isClosed = false
    var position = 0L

    override def length = owner.size(path)

    override def close() = if (!isClosed) {
      isClosed = true
      owner.handles -= handle
      stream.close()
    }

    override def read(into: Array[Byte]) = {
      val read = stream.read(into)
      if (read >= 0)
        position += read
      read
    }

    override def seek(to: Long) = {
      stream.reset()
      position = stream.skip(to)
      position
    }

    override def write(value: Array[Byte]) = throw new IOException("bad file descriptor")
  }

}
