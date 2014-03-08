package li.cil.oc.server.fs

import li.cil.oc.api
import li.cil.oc.api.fs.Mode
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.NBTTagCompound
import scala.collection.mutable
import java.io.FileNotFoundException

class CompositeReadOnlyFileSystem extends api.fs.FileSystem {
  var parts = mutable.LinkedHashMap.empty[String, api.fs.FileSystem]

  // ----------------------------------------------------------------------- //

  override def isReadOnly = true

  override def spaceTotal = parts.values.map(_.spaceTotal).sum

  override def spaceUsed = parts.values.map(_.spaceUsed).sum

  // ----------------------------------------------------------------------- //

  override def exists(path: String) = findFileSystem(path).isDefined

  override def size(path: String) = findFileSystem(path).fold(0L)(_.size(path))

  override def isDirectory(path: String) = findFileSystem(path).fold(false)(_.isDirectory(path))

  override def lastModified(path: String) = findFileSystem(path).fold(0L)(_.lastModified(path))

  override def list(path: String) = findFileSystem(path).fold(null: Array[String])(_.list(path))

  // ----------------------------------------------------------------------- //

  override def delete(path: String) = false

  override def makeDirectory(path: String) = false

  override def rename(from: String, to: String) = false

  override def setLastModified(path: String, time: Long) = false

  // ----------------------------------------------------------------------- //

  override def open(path: String, mode: Mode) = findFileSystem(path) match {
    case Some(fs) => fs.open(path, mode)
    case _ => throw new FileNotFoundException()
  }

  override def getHandle(handle: Int) = parts.valuesIterator.map(_.getHandle(handle)).find(_ != null).orNull

  override def close() = parts.values.foreach(_.close())

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    for ((name, fs) <- parts) {
      fs.load(nbt.getCompoundTag(name))
    }
  }

  override def save(nbt: NBTTagCompound) {
    for ((name, fs) <- parts) {
      nbt.setNewCompoundTag(name, fs.save)
    }
  }

  // ----------------------------------------------------------------------- //

  protected def findFileSystem(path: String): Option[api.fs.FileSystem] = parts.valuesIterator.toSeq.reverse.find(_.exists(path))
}
