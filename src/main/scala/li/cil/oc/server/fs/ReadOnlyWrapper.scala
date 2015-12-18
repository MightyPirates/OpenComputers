package li.cil.oc.server.fs

import java.io.FileNotFoundException

import li.cil.oc.api
import li.cil.oc.api.fs.Mode
import net.minecraft.nbt.NBTTagCompound

private class ReadOnlyWrapper(val fileSystem: api.fs.FileSystem) extends api.fs.FileSystem {
  override def isReadOnly = true

  override def spaceTotal = fileSystem.spaceUsed()

  override def spaceUsed = fileSystem.spaceUsed()

  override def exists(path: String) = fileSystem.exists(path)

  override def size(path: String) = fileSystem.size(path)

  override def isDirectory(path: String) = fileSystem.isDirectory(path)

  override def lastModified(path: String) = fileSystem.lastModified(path)

  override def list(path: String) = fileSystem.list(path)

  override def delete(path: String) = false

  override def makeDirectory(path: String) = false

  override def rename(from: String, to: String) = false

  override def setLastModified(path: String, time: Long) = false

  override def open(path: String, mode: Mode) = mode match {
    case Mode.Read => fileSystem.open(path, mode)
    case Mode.Write => throw new FileNotFoundException("Read-only filesystem; cannot open for writing: " + path)
    case Mode.Append => throw new FileNotFoundException("Read-only filesystem; cannot open for appending: " + path)
  }

  override def getHandle(handle: Int) = fileSystem.getHandle(handle)

  override def close() = fileSystem.close()

  override def load(nbt: NBTTagCompound) = fileSystem.load(nbt)

  override def save(nbt: NBTTagCompound) = fileSystem.save(nbt)
}
