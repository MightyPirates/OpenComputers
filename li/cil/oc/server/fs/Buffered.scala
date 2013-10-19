package li.cil.oc.server.fs

import java.io
import li.cil.oc.api.fs.Mode
import net.minecraft.nbt.NBTTagCompound
import org.apache.commons.io.FileUtils
import scala.collection.mutable

trait Buffered extends OutputStreamFileSystem {
  protected def fileRoot: io.File

  private val deletions = mutable.Map.empty[String, Long]

  // ----------------------------------------------------------------------- //

  override def rename(from: String, to: String) = {
    if (super.rename(from, to)) {
      deletions += from -> System.currentTimeMillis()
      true
    }
    else false
  }

  override protected def delete(path: String) = {
    if (super.delete(path)) {
      deletions += path -> System.currentTimeMillis()
      true
    }
    else false
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)

    def recurse(path: String, directory: io.File) {
      makeDirectories(path)
      for (child <- directory.listFiles()) {
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
              setLastModified(childPath, childFile.lastModified())
            case _ => // File is open for writing.
          }
        }
      }
      setLastModified(path, directory.lastModified())
    }
    recurse("", fileRoot)
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)

    for ((path, time) <- deletions) {
      val file = new io.File(fileRoot, path)
      if (FileUtils.isFileOlder(file, time))
        FileUtils.deleteQuietly(file)
    }
    deletions.clear()

    def recurse(path: String) {
      val directory = new io.File(fileRoot, path)
      directory.mkdirs()
      for (child <- list(path).get) {
        val childPath = path + child
        if (isDirectory(childPath))
          recurse(childPath)
        else {
          val childFile = new io.File(fileRoot, childPath)
          val time = lastModified(childPath)
          if (time == 0 || FileUtils.isFileOlder(childFile, time)) {
            FileUtils.deleteQuietly(childFile)
            childFile.createNewFile()
            val out = new io.FileOutputStream(childFile).getChannel
            val in = java.nio.channels.Channels.newChannel(openInputStream(childPath).get)
            out.transferFrom(in, 0, Long.MaxValue)
            out.close()
            in.close()
            childFile.setLastModified(time)
          }
        }
      }
      directory.setLastModified(lastModified(path))
    }
    recurse("")
  }
}
