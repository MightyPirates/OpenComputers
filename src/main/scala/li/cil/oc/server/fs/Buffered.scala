package li.cil.oc.server.fs

import java.io
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.util.concurrent.CancellationException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import li.cil.oc.OpenComputers
import li.cil.oc.api.fs.Mode
import li.cil.oc.util.ThreadPoolFactory
import li.cil.oc.util.SafeThreadPool
import net.minecraft.nbt.CompoundNBT
import org.apache.commons.io.FileUtils

import scala.collection.mutable

object Buffered {
  val fileSaveHandler: SafeThreadPool = ThreadPoolFactory.createSafePool("FileSystem", 1)
}

trait Buffered extends OutputStreamFileSystem {
  protected def fileRoot: io.File

  private val deletions = mutable.Map.empty[String, Long]

  // ----------------------------------------------------------------------- //

  override def delete(path: String): Boolean = {
    if (super.delete(path)) {
      deletions += path -> System.currentTimeMillis()
      true
    }
    else false
  }

  override def rename(from: String, to: String): Boolean = {
    if (super.rename(from, to)) {
      deletions += from -> System.currentTimeMillis()
      true
    }
    else false
  }

  // ----------------------------------------------------------------------- //

  private var saving: Option[Future[_]] = None

  override def loadData(nbt: CompoundNBT): Unit = {
    saving.foreach(f => try {
      f.get(120L, TimeUnit.SECONDS)
    } catch {
      case e: TimeoutException => OpenComputers.log.warn("Waiting for filesystem to save took two minutes! Aborting.")
      case e: CancellationException => // NO-OP
    })
    loadFiles(nbt)
    super.loadData(nbt)
  }

  private def loadFiles(nbt: CompoundNBT): Unit = this.synchronized {
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
  }

  override def saveData(nbt: CompoundNBT): Unit = {
    super.saveData(nbt)
    saving = Buffered.fileSaveHandler.withPool(_.submit(new Runnable {
      override def run(): Unit = saveFiles()
    }))
  }

  def saveFiles(): Unit = this.synchronized {
    for ((path, time) <- deletions) {
      val file = new io.File(fileRoot, path)
      if (FileUtils.isFileOlder(file, time))
        FileUtils.deleteQuietly(file)
    }
    deletions.clear()

    val buffer = ByteBuffer.allocateDirect(16 * 1024)
    def recurse(path: String):Boolean = {
      val directory = new io.File(fileRoot, path)
      directory.mkdirs()
      var dirChanged = false
      for (child <- list(path)) {
        val childPath = path + child
        if (isDirectory(childPath))
          dirChanged = recurse(childPath) || dirChanged
        else {
          val childFile = new io.File(fileRoot, childPath)
          val time = lastModified(childPath)
          if (time == 0 || !childFile.exists() || FileUtils.isFileOlder(childFile, time)) {
            FileUtils.deleteQuietly(childFile)
            childFile.createNewFile()
            val out = new io.FileOutputStream(childFile).getChannel
            val in = openInputChannel(childPath).get

            buffer.clear()
            while (in.read(buffer) != -1) {
              buffer.flip()
              out.write(buffer)
              buffer.compact()
            }

            buffer.flip()
            while (buffer.hasRemaining) {
              out.write(buffer)
            }

            out.close()
            in.close()
            childFile.setLastModified(time)
            dirChanged = true
          }
        }
      }
      if (dirChanged) {
        directory.setLastModified(lastModified(path))
        true
      } else false
    }
    if (list("") == null || list("").isEmpty) {
      fileRoot.delete()
    }
    else recurse("")
  }
}
