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
    val freed = Settings.Filesystem.fileCost + size(path)
    if (super.delete(path)) {
      used = math.max(0, used - freed)
      true
    }
    else false
  }

  override def makeDirectory(path: String) = {
    if (capacity - used < Settings.Filesystem.fileCost && !ignoreCapacity) {
      throw new io.IOException("not enough space")
    }
    if (super.makeDirectory(path)) {
      used += Settings.Filesystem.fileCost
      true
    }
    else false
  }

  // ----------------------------------------------------------------------- //

  override def close() {
    super.close()
    used = computeSize("/")
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    try {
      ignoreCapacity = true
      super.load(nbt)
    } finally {
      ignoreCapacity = false
    }

    used = computeSize("/")
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)

    // For the tooltip.
    nbt.setLong("capacity.used", used)
  }

  // ----------------------------------------------------------------------- //

  override abstract protected def openOutputHandle(id: Int, path: String, mode: Mode): Option[OutputHandle] = {
    val delta =
      if (exists(path))
        if (mode == Mode.Write)
          -size(path) // Overwrite, file gets cleared.
        else
          0 // Append, no immediate changes.
      else
        Settings.Filesystem.fileCost // File creation.
    if (capacity - used < delta && !ignoreCapacity) {
      throw new io.IOException("not enough space")
    }
    super.openOutputHandle(id, path, mode) match {
      case None => None
      case Some(stream) =>
        used = math.max(0, used + delta)
        if (mode == Mode.Append) {
          stream.seek(stream.length())
        }
        Some(new CountingOutputHandle(this, stream))
    }
  }

  // ----------------------------------------------------------------------- //

  private def computeSize(path: String): Long =
    Settings.Filesystem.fileCost +
      size(path) +
      (if (isDirectory(path))
        list(path).foldLeft(0L)((acc, child) => acc + computeSize(path + child))
      else 0)

  protected class CountingOutputHandle(override val owner: Capacity, val inner: OutputHandle) extends OutputHandle(inner.owner, inner.handle, inner.path) {
    override def isClosed = inner.isClosed

    override def length = inner.length

    override def position = inner.position

    override def close() = inner.close()

    override def seek(to: Long) = inner.seek(to)

    override def write(b: Array[Byte]) {
      if (owner.capacity - owner.used < b.length && !ignoreCapacity)
        throw new io.IOException("not enough space")
      inner.write(b)
      owner.used = owner.used + b.length
    }
  }

}
