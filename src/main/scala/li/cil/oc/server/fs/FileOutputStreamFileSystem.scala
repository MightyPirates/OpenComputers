package li.cil.oc.server.fs

import java.io
import java.io.RandomAccessFile

import li.cil.oc.api.fs.Mode
import net.minecraft.nbt.NBTTagCompound

trait FileOutputStreamFileSystem extends FileInputStreamFileSystem with OutputStreamFileSystem {
  override def spaceTotal = -1

  override def spaceUsed = -1

  // ----------------------------------------------------------------------- //

  override def delete(path: String) = {
    val file = new io.File(root, FileSystem.validatePath(path))
    file == root || file.delete()
  }

  override def makeDirectory(path: String) = new io.File(root, FileSystem.validatePath(path)).mkdir()

  override def rename(from: String, to: String) = new io.File(root, FileSystem.validatePath(from)).renameTo(new io.File(root, FileSystem.validatePath(to)))

  override def setLastModified(path: String, time: Long) = new io.File(root, FileSystem.validatePath(path)).setLastModified(time)

  // ----------------------------------------------------------------------- //

  override protected def openOutputHandle(id: Int, path: String, mode: Mode): Option[OutputHandle] =
    Some(new FileHandle(new RandomAccessFile(new io.File(root, path), mode match {
      case Mode.Append | Mode.Write => "rw"
      case _ => throw new IllegalArgumentException()
    }), this, id, path, mode))

  // ----------------------------------------------------------------------- //

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    root.mkdirs()
    root.setLastModified(System.currentTimeMillis())
  }

  // ----------------------------------------------------------------------- //

  protected class FileHandle(val file: RandomAccessFile, owner: OutputStreamFileSystem, handle: Int, path: String, mode: Mode) extends OutputHandle(owner, handle, path) {
    if (mode == Mode.Write) {
      file.setLength(0)
    }

    override def position() = file.getFilePointer

    override def length() = file.length()

    override def close() {
      super.close()
      file.close()
    }

    override def seek(to: Long) = {
      file.seek(to)
      to
    }

    override def write(value: Array[Byte]) = file.write(value)
  }

}
