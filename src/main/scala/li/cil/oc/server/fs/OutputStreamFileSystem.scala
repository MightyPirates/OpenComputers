package li.cil.oc.server.fs

import java.io.FileNotFoundException
import java.io.IOException

import li.cil.oc.api
import li.cil.oc.api.fs.Mode
import net.minecraft.nbt.CompoundNBT
import net.minecraft.nbt.ListNBT
import net.minecraftforge.common.util.Constants.NBT

import scala.collection.mutable

trait OutputStreamFileSystem extends InputStreamFileSystem {
  private val handles = mutable.Map.empty[Int, OutputHandle]

  // ----------------------------------------------------------------------- //

  override def isReadOnly = false

  // ----------------------------------------------------------------------- //

  override def open(path: String, mode: Mode) = this.synchronized(mode match {
    case Mode.Read => super.open(path, mode)
    case _ =>
      FileSystem.validatePath(path)
      if (!isDirectory(path)) {
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

  private final val OutputTag = "output"
  private final val HandleTag = "handle"
  private final val PathTag = "path"

  override def loadData(nbt: CompoundNBT) {
    super.loadData(nbt)

    val handlesNbt = nbt.getList(OutputTag, NBT.TAG_COMPOUND)
    (0 until handlesNbt.size).map(handlesNbt.getCompound).foreach(handleNbt => {
      val handle = handleNbt.getInt(HandleTag)
      val path = handleNbt.getString(PathTag)
      openOutputHandle(handle, path, Mode.Append) match {
        case Some(fileHandle) => handles += handle -> fileHandle
        case _ => // The source file seems to have changed since last time.
      }
    })
  }

  override def saveData(nbt: CompoundNBT): Unit = this.synchronized {
    super.saveData(nbt)

    val handlesNbt = new ListNBT()
    for (file <- handles.values) {
      assert(!file.isClosed)
      val handleNbt = new CompoundNBT()
      handleNbt.putInt(HandleTag, file.handle)
      handleNbt.putString(PathTag, file.path)
      handlesNbt.add(handleNbt)
    }
    nbt.put(OutputTag, handlesNbt)
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
