package li.cil.oc.server.fs

import java.io
import li.cil.oc.Settings
import li.cil.oc.api.fs.Mode
import net.minecraft.nbt.NBTTagCompound

trait Capacity extends OutputStreamFileSystem {
  private var used = computeSize("/")

  // Used when loading data from disk to virtual file systems, to allow
  // exceeding the actual capacity of a file system.
  private var ignoreCapacity = false

  protected def capacity: Long

  // ----------------------------------------------------------------------- //

  override def spaceTotal = capacity

  override def spaceUsed = used

  // ----------------------------------------------------------------------- //

  override def delete(path: String) = {
    val freed = Settings.get.fileCost + size(path)
    if (super.delete(path)) {
      used -= freed
      true
    }
    else false
  }

  override def makeDirectory(path: String) = {
    if (capacity - used < Settings.get.fileCost && !ignoreCapacity) {
      throw new io.IOException("not enough space")
    }
    if (super.makeDirectory(path)) {
      used += Settings.get.fileCost
      true
    }
    else false
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    try {
      ignoreCapacity = true
      super.load(nbt)
    } finally {
      ignoreCapacity = false
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)

    // For the tooltip.
    nbt.setLong("capacity.used", used)
  }

  // ----------------------------------------------------------------------- //

  override abstract protected def openOutputStream(path: String, mode: Mode): Option[io.OutputStream] = {
    val delta =
      if (exists(path))
        if (mode == Mode.Write)
          -size(path) // Overwrite, file gets cleared.
        else
          0 // Append, no immediate changes.
      else
        Settings.get.fileCost // File creation.
    super.openOutputStream(path, mode) match {
      case None => None
      case Some(stream) =>
        used += delta
        Some(new CountingOutputStream(this, stream))
    }
  }

  // ----------------------------------------------------------------------- //

  private def computeSize(path: String): Long =
    Settings.get.fileCost +
      size(path) +
      (if (isDirectory(path))
        list(path).foldLeft(0L)((acc, child) => acc + computeSize(path + child))
      else 0)

  private class CountingOutputStream(val owner: Capacity, val inner: io.OutputStream) extends io.OutputStream {
    override def write(b: Int) = {
      if (owner.capacity - owner.used < 1 && !ignoreCapacity)
        throw new io.IOException("not enough space")
      inner.write(b)
      owner.used = owner.used + 1
    }

    override def write(b: Array[Byte], off: Int, len: Int) = {
      if (owner.capacity - owner.used < len && !ignoreCapacity)
        throw new io.IOException("not enough space")
      inner.write(b, off, len)
      owner.used = owner.used + len
    }

    override def flush() = inner.flush()

    override def close() = inner.close()
  }

}