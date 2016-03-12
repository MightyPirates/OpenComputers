package li.cil.oc.server.fs

import java.io.FileNotFoundException
import java.util.concurrent.Callable

import li.cil.oc.api
import li.cil.oc.api.fs.Mode
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.NBTTagCompound

import scala.collection.mutable

class CompositeReadOnlyFileSystem(factories: mutable.LinkedHashMap[String, Callable[api.fs.FileSystem]]) extends api.fs.FileSystem {
  var parts = mutable.LinkedHashMap.empty[String, api.fs.FileSystem]
  for ((name, factory) <- factories) {
    val fs = factory.call()
    if (fs != null) {
      parts += name -> fs
    }
  }

  // ----------------------------------------------------------------------- //

  override def isReadOnly = true

  override def spaceTotal = math.max(spaceUsed, parts.values.map(_.spaceTotal).sum)

  override def spaceUsed = parts.values.map(_.spaceUsed).sum

  // ----------------------------------------------------------------------- //

  override def exists(path: String) = findFileSystem(path).isDefined

  override def size(path: String) = findFileSystem(path).fold(0L)(_.size(path))

  override def isDirectory(path: String) = findFileSystem(path).fold(false)(_.isDirectory(path))

  override def lastModified(path: String) = findFileSystem(path).fold(0L)(_.lastModified(path))

  override def list(path: String) = if (isDirectory(path)) {
    parts.values.foldLeft(mutable.Set.empty[String])((acc, fs) => {
      if (fs.exists(path)) try {
        val l = fs.list(path)
        if (l != null) for (e <- l) {
          val f = e.stripSuffix("/")
          val d = f + "/"
          // Avoid duplicates and always only use the latest entry.
          acc -= f
          acc -= d
          acc += e
        }
      }
      catch {
        case _: Throwable =>
      }
      acc
    }).toArray
  }
  else null

  // ----------------------------------------------------------------------- //

  override def delete(path: String) = false

  override def makeDirectory(path: String) = false

  override def rename(from: String, to: String) = false

  override def setLastModified(path: String, time: Long) = false

  // ----------------------------------------------------------------------- //

  override def open(path: String, mode: Mode) = findFileSystem(path) match {
    case Some(fs) => fs.open(path, mode)
    case _ => throw new FileNotFoundException(path)
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
