package li.cil.oc.server.fs

import java.io.FileNotFoundException
import java.io.IOException

import li.cil.oc.api
import li.cil.oc.api.fs.Mode
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraftforge.common.util.Constants.NBT

import scala.collection.mutable

trait OutputStreamFileSystem extends InputStreamFileSystem {
  private val handles = mutable.Map.empty[Int, OutputHandle]

  // ----------------------------------------------------------------------- //

  override def isReadOnly = false

  // ----------------------------------------------------------------------- //

  override def open(path: String, mode: Mode) = this.synchronized(mode match {
    case Mode.Read => super.open(path, mode)
    case _ => if (!isDirectory(path)) {
      val handle = Iterator.continually((Math.random() * Int.MaxValue).toInt + 1).filterNot(handles.contains).next()
      openOutputHandle(handle, path, mode) match {
        case Some(fileHandle) =>
          handles += handle -> fileHandle
          handle
        case _ => throw new FileNotFoundException(path)
      }
    } else throw new FileNotFoundException(path)
  })

  override def getHandle(handle: Int): api.fs.Handle = this.synchronized(Option(super.getHandle(handle)).orElse(handles.get(handle)).orNull)

  override def close() = this.synchronized {
    super.close()
    for (handle <- handles.values)
      handle.close()
    handles.clear()
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)

    val handlesNbt = nbt.getTagList("output", NBT.TAG_COMPOUND)
    (0 until handlesNbt.tagCount).map(handlesNbt.getCompoundTagAt).foreach(handleNbt => {
      val handle = handleNbt.getInteger("handle")
      val path = handleNbt.getString("path")
      openOutputHandle(handle, path, Mode.Append) match {
        case Some(fileHandle) => handles += handle -> fileHandle
        case _ => // The source file seems to have changed since last time.
      }
    })
  }

  override def save(nbt: NBTTagCompound) = this.synchronized {
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

  protected def openOutputHandle(id: Int, path: String, mode: Mode): Option[OutputHandle]

  // ----------------------------------------------------------------------- //

  protected abstract class OutputHandle(val owner: OutputStreamFileSystem, val handle: Int, val path: String) extends api.fs.Handle {
    protected var _isClosed = false

    def isClosed = _isClosed

    override def close() = if (!isClosed) {
      _isClosed = true
      owner.handles -= handle
    }

    override def read(into: Array[Byte]): Int = throw new IOException("bad file descriptor")

    override def seek(to: Long): Long = throw new IOException("bad file descriptor")
  }

}
