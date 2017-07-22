package li.cil.oc.server.fs

import java.io
import java.io.FileNotFoundException

import li.cil.oc.api.fs.Mode
import net.minecraft.nbt.NBTTagCompound
import org.apache.commons.io.FileUtils

import scala.collection.mutable

trait Buffered extends OutputStreamFileSystem {
  protected def fileRoot: io.File

  private val deletions = mutable.Map.empty[String, Long]

  // ----------------------------------------------------------------------- //

  override def delete(path: String) = {
    if (super.delete(path)) {
      deletions += path -> System.currentTimeMillis()
      true
    }
    else false
  }

  override def rename(from: String, to: String) = {
    if (super.rename(from, to)) {
      deletions += from -> System.currentTimeMillis()
      true
    }
    else false
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) = {
    def recurse(path: String, directory: io.File) {
      makeDirectory(path)
      for (child <- directory.listFiles() if FileSystem.isValidFilename(child.getName)) {
        val childPath = path + child.getName
        val childFile = new io.File(directory, child.getName)
        if (child.exists() && child.isDirectory && child.list() != null) {
          recurse(childPath + "/", childFile)
        }
        else if (!exists(childPath) || !isDirectory(childPath)) {
          openOutputHandle(0, childPath, Mode.Write) match {
            case Some(stream) =>
              try {
                val in = new io.FileInputStream(childFile)
                val buffer = new Array[Byte](8 * 1024)
                var read = 0
                do {
                  read = in.read(buffer)
                  if (read > 0) {
                    if (read == buffer.length) stream.write(buffer)
                    else stream.write(buffer.view(0, read).toArray)
                  }
                } while (read >= 0)
                in.close()
              }
              catch {
                case _: FileNotFoundException => // File got deleted in the meantime.
              }
              stream.close()
              setLastModified(childPath, childFile.lastModified())
            case _ => // File is open for writing.
          }
        }
      }
      setLastModified(path, directory.lastModified())
    }
    if (fileRoot.list() == null || fileRoot.list().isEmpty) {
      fileRoot.delete()
    }
    else recurse("", fileRoot)

    super.load(nbt)
  }

  override def save(nbt: NBTTagCompound) = {
    super.save(nbt)

    for ((path, time) <- deletions) {
      val file = new io.File(fileRoot, path)
      if (FileUtils.isFileOlder(file, time))
        FileUtils.deleteQuietly(file)
    }
    deletions.clear()

    def recurse(path: String) {
      val directory = new io.File(fileRoot, path)
      directory.mkdirs()
      for (child <- list(path)) {
        val childPath = path + child
        if (isDirectory(childPath))
          recurse(childPath)
        else {
          val childFile = new io.File(fileRoot, childPath)
          val time = lastModified(childPath)
          if (time == 0 || !childFile.exists() || FileUtils.isFileOlder(childFile, time)) {
            FileUtils.deleteQuietly(childFile)
            childFile.createNewFile()
            val out = new io.FileOutputStream(childFile).getChannel
            val in = openInputChannel(childPath).get
            out.transferFrom(in, 0, Long.MaxValue)
            out.close()
            in.close()
            childFile.setLastModified(time)
          }
        }
      }
      directory.setLastModified(lastModified(path))
    }
    if (list("") == null || list("").isEmpty) {
      fileRoot.delete()
    }
    else recurse("")
  }
}
