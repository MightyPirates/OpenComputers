package li.cil.oc.server.fs

import java.io.{FileNotFoundException, IOException, OutputStream}
import li.cil.oc.api
import li.cil.oc.api.fs.Mode
import net.minecraft.nbt.{NBTTagList, NBTTagCompound}
import scala.collection.mutable

trait OutputStreamFileSystem extends InputStreamFileSystem {
  private val handles = mutable.Map.empty[Int, Handle]

  // ----------------------------------------------------------------------- //

  override def open(path: String, mode: Mode) = mode match {
    case Mode.Read => super.open(path, mode)
    case _ => if (!isDirectory(path)) {
      val handle = Iterator.continually((Math.random() * Int.MaxValue).toInt + 1).filterNot(handles.contains).next()
      openOutputStream(path, mode) match {
        case Some(stream) =>
          handles += handle -> new Handle(this, handle, path, stream)
          handle
        case _ => throw new FileNotFoundException()
      }
    } else throw new FileNotFoundException()
  }

  override def file(handle: Int): api.fs.Handle = Option(super.file(handle)).orElse(handles.get(handle)).orNull

  override def close() {
    super.close()
    for (handle <- handles.values)
      handle.close()
    handles.clear()
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)

    val handlesNbt = nbt.getTagList("output")
    (0 until handlesNbt.tagCount).map(handlesNbt.tagAt).map(_.asInstanceOf[NBTTagCompound]).foreach(handleNbt => {
      val handle = handleNbt.getInteger("handle")
      val path = handleNbt.getString("path")
      openOutputStream(path, Mode.Append) match {
        case Some(stream) =>
          val fileHandle = new Handle(this, handle, path, stream)
          handles += handle -> fileHandle
        case _ => // The source file seems to have changed since last time.
      }
    })
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)

    val handlesNbt = new NBTTagList()
    for (file <- handles.values) {
      assert(!file.isClosed)
      val handleNbt = new NBTTagCompound()
      handleNbt.setInteger("handle", file.handle)
      handleNbt.setString("path", file.path)
      handlesNbt.appendTag(handleNbt)
    }
    nbt.setTag("output", handlesNbt)
  }

  // ----------------------------------------------------------------------- //

  protected def openOutputStream(path: String, mode: Mode): Option[OutputStream]

  // ----------------------------------------------------------------------- //

  private class Handle(val owner: OutputStreamFileSystem, val handle: Int, val path: String, val stream: OutputStream) extends api.fs.Handle {
    var isClosed = false
    val position = 0L
    val length = 0L

    def close() = if (!isClosed) {
      isClosed = true
      owner.handles -= handle
      stream.close()
    }

    def read(into: Array[Byte]) = throw new IOException("bad file descriptor")

    def seek(to: Long) = throw new IOException("bad file descriptor")

    def write(value: Array[Byte]) {
      stream.write(value)
    }
  }

}
