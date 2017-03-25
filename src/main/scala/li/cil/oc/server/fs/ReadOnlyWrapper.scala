package li.cil.oc.server.fs

import java.io.FileNotFoundException

import li.cil.oc.api
import li.cil.oc.api.fs.Handle
import li.cil.oc.api.fs.Mode
import net.minecraft.nbt.NBTTagCompound

private class ReadOnlyWrapper(val fileSystem: api.fs.FileSystem) extends api.fs.FileSystem {
  override def isReadOnly = true

  override def spaceTotal: Long = fileSystem.spaceUsed()

  override def spaceUsed: Long = fileSystem.spaceUsed()

  override def exists(path: String): Boolean = fileSystem.exists(path)

  override def size(path: String): Long = fileSystem.size(path)

  override def isDirectory(path: String): Boolean = fileSystem.isDirectory(path)

  override def lastModified(path: String): Long = fileSystem.lastModified(path)

  override def list(path: String): Array[String] = fileSystem.list(path)

  override def delete(path: String) = false

  override def makeDirectory(path: String) = false

  override def rename(from: String, to: String) = false

  override def setLastModified(path: String, time: Long) = false

  override def open(path: String, mode: Mode): Int = mode match {
    case Mode.Read => fileSystem.open(path, mode)
    case Mode.Write => throw new FileNotFoundException("read-only filesystem; cannot open for writing: " + path)
    case Mode.Append => throw new FileNotFoundException("read-only filesystem; cannot open for appending: " + path)
  }

  override def getHandle(handle: Int): Handle = fileSystem.getHandle(handle)

  override def close(): Unit = fileSystem.close()

  override def serializeNBT(): NBTTagCompound = fileSystem.serializeNBT()

  override def deserializeNBT(nbt: NBTTagCompound): Unit = fileSystem.deserializeNBT(nbt)
}
