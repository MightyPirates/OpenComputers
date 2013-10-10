package li.cil.oc.server.fs

import dan200.computer.api.{IWritableMount, IMount}
import java.io
import java.util.zip.ZipFile
import li.cil.oc.server.component
import li.cil.oc.{Config, api}
import net.minecraftforge.common.DimensionManager

object FileSystem extends api.detail.FileSystemAPI {
  override def fromClass(clazz: Class[_], domain: String, root: String): Option[api.FileSystem] = {
    val codeSource = clazz.getProtectionDomain.getCodeSource
    if (codeSource == null) return None
    val file = new io.File(codeSource.getLocation.toURI)
    if (!file.exists || file.isDirectory) return None
    val path = ("/assets/" + domain + "/" + (root.trim + "/")).replace("//", "/")
    if (file.getName.endsWith(".class"))
      new io.File(new io.File(file.getParent), path) match {
        case fsp if fsp.exists() && fsp.isDirectory =>
          Some(new ReadOnlyFileSystem(fsp))
        case _ =>
          System.getProperty("java.class.path").split(System.getProperty("path.separator")).
            find(cp => {
            val fsp = new io.File(new io.File(cp), path)
            fsp.exists() && fsp.isDirectory
          }) match {
            case None => None
            case Some(dir) => Some(new ReadOnlyFileSystem(new io.File(new io.File(dir), path)))
          }
      }
    else {
      val zip = new ZipFile(file)
      val entry = zip.getEntry(path)
      if (entry == null || !entry.isDirectory) {
        zip.close()
        return None
      }
      Some(new ZipFileInputStreamFileSystem(zip, path))
    }
  }

  override def fromSaveDirectory(root: String, capacity: Long, buffered: Boolean) = {
    val path = new io.File(DimensionManager.getCurrentSaveRootDirectory, Config.savePath + root)
    path.mkdirs()
    if (path.exists() && path.isDirectory) {
      if (buffered)
        Some(new BufferedFileSystem(path, capacity))
      else
        Some(new ReadWriteFileSystem(path, capacity))
    }
    else None
  }

  override def fromMemory(capacity: Long): Option[api.FileSystem] = Some(new RamFileSystem(capacity))

  def fromComputerCraft(mount: IMount) = Some(new ComputerCraftFileSystem(mount))

  def fromComputerCraft(mount: IWritableMount) = Some(new ComputerCraftWritableFileSystem(mount))

  override def asNode(fileSystem: api.FileSystem) = Some(new component.FileSystem(fileSystem))

  private class ReadOnlyFileSystem(protected val root: io.File)
    extends InputStreamFileSystem
    with FileInputStreamFileSystem

  private class ReadWriteFileSystem(protected val root: io.File, protected val capacity: Long)
    extends OutputStreamFileSystem
    with FileOutputStreamFileSystem
    with Capacity

  private class RamFileSystem(protected val capacity: Long)
    extends VirtualFileSystem
    with Capacity

  private class BufferedFileSystem(protected val fileRoot: io.File, capacity: Long)
    extends RamFileSystem(capacity)
    with Buffered

}
