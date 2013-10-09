package li.cil.oc.server.fs

import java.io
import li.cil.oc.api.fs.Mode
import net.minecraft.nbt.NBTTagCompound
import scala.collection.mutable

trait Buffered extends OutputStreamFileSystem {
  protected def fileRoot: io.File

  private val deletions = mutable.Set.empty[String]

  // ----------------------------------------------------------------------- //

  override def rename(from: String, to: String) = {
    if (super.rename(from, to)) {
      deletions += from
      true
    }
    else false
  }

  override protected def delete(path: String) = {
    if (super.delete(path)) {
      deletions += path
      true
    }
    else false
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) = {
    super.load(nbt)
    def recurse(path: String, directory: io.File) {
      makeDirectories(path)
      if (isDirectory(path)) for (child <- directory.listFiles()) {
        val childPath = path + child.getName
        val childFile = new io.File(directory, child.getName)
        if (child.isDirectory) {
          recurse(childPath + "/", childFile)
        }
        else if (!exists(childPath) || !isDirectory(childPath)) {
          openOutputStream(childPath, Mode.Write) match {
            case Some(stream) =>
              val in = new io.FileInputStream(childFile).getChannel
              val out = java.nio.channels.Channels.newChannel(stream)
              in.transferTo(0, Long.MaxValue, out)
              in.close()
              out.close()
            case _ => // File is open for writing.
          }
        }
      }
    }
    recurse("", fileRoot)
  }

  override def save(nbt: NBTTagCompound) = {
    super.save(nbt)

    for (path <- deletions)
      org.apache.commons.io.FileUtils.deleteQuietly(new io.File(fileRoot, path))
    deletions.clear()

    def recurse(path: String) {
      val directory = new io.File(fileRoot, path)
      if (directory.exists() && !directory.isDirectory)
        org.apache.commons.io.FileUtils.deleteQuietly(directory)
      directory.mkdirs()
      for (child <- list(path).get) {
        val childPath = path + child
        if (isDirectory(childPath))
          recurse(childPath)
        else {
          val childFile = new io.File(fileRoot, childPath)
          org.apache.commons.io.FileUtils.deleteQuietly(childFile)
          childFile.createNewFile()
          val out = new io.FileOutputStream(childFile).getChannel
          val in = java.nio.channels.Channels.newChannel(openInputStream(childPath).get)
          out.transferFrom(in, 0, Long.MaxValue)
          out.close()
          in.close()
        }
      }
    }
    recurse("")
  }
}
