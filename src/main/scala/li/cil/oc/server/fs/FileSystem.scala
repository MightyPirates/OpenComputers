package li.cil.oc.server.fs

import java.io
import java.net.URL

import li.cil.oc.api.driver.Container
import li.cil.oc.api.fs.{Label, Mode}
import li.cil.oc.server.component
import li.cil.oc.util.mods.{ComputerCraft, Mods}
import li.cil.oc.{Settings, api}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.DimensionManager

object FileSystem extends api.detail.FileSystemAPI {
  override def fromClass(clazz: Class[_], domain: String, root: String): api.fs.FileSystem = {
    val innerPath = ("/assets/" + domain + "/" + (root.trim + "/")).replace("//", "/")

    val codeSource = clazz.getProtectionDomain.getCodeSource.getLocation.getPath
    val (codeUrl, isArchive) =
      if (codeSource.contains(".zip!") || codeSource.contains(".jar!"))
        (codeSource.substring(0, codeSource.lastIndexOf('!')), true)
      else
        (codeSource, false)

    val file = try {
      val url = new URL(codeUrl)
      try {
        new io.File(url.toURI)
      }
      catch {
        case _: Throwable => new io.File(url.getPath)
      }
    } catch {
      case _: Throwable => new io.File(codeSource)
    }

    if (isArchive) {
      ZipFileInputStreamFileSystem.fromFile(file, innerPath.substring(1))
    }
    else {
      if (!file.exists || file.isDirectory) return null
      new io.File(new io.File(file.getParent), innerPath) match {
        case fsp if fsp.exists() && fsp.isDirectory =>
          new ReadOnlyFileSystem(fsp)
        case _ =>
          System.getProperty("java.class.path").split(System.getProperty("path.separator")).
            find(cp => {
            val fsp = new io.File(new io.File(cp), innerPath)
            fsp.exists() && fsp.isDirectory
          }) match {
            case None => null
            case Some(dir) => new ReadOnlyFileSystem(new io.File(new io.File(dir), innerPath))
          }
      }
    }
  }

  override def fromSaveDirectory(root: String, capacity: Long, buffered: Boolean) = {
    val path = new io.File(DimensionManager.getCurrentSaveRootDirectory, Settings.savePath + root)
    path.mkdirs()
    if (path.exists() && path.isDirectory) {
      if (buffered) new BufferedFileSystem(path, capacity)
      else new ReadWriteFileSystem(path, capacity)
    }
    else null
  }

  def fromMemory(capacity: Long): api.fs.FileSystem = new RamFileSystem(capacity)

  def fromComputerCraft(mount: AnyRef): api.fs.FileSystem = {
    if (Mods.ComputerCraft.isAvailable) {
      ComputerCraft.createFileSystem(mount).orNull
    }
    else null
  }

  def asManagedEnvironment(fileSystem: api.fs.FileSystem, label: Label, container: Container) =
    Option(fileSystem).flatMap(fs => Some(new component.FileSystem(fs, label, Option(container)))).orNull

  def asManagedEnvironment(fileSystem: api.fs.FileSystem, label: String, container: Container) =
    asManagedEnvironment(fileSystem, new ReadOnlyLabel(label), container)

  def asManagedEnvironment(fileSystem: api.fs.FileSystem, label: Label) =
    Option(fileSystem).flatMap(fs => Some(new component.FileSystem(fs, label))).orNull

  def asManagedEnvironment(fileSystem: api.fs.FileSystem, label: String) =
    asManagedEnvironment(fileSystem, new ReadOnlyLabel(label))

  def asManagedEnvironment(fileSystem: api.fs.FileSystem) =
    asManagedEnvironment(fileSystem, null: Label)

  private class ReadOnlyLabel(val label: String) extends Label {
    def setLabel(value: String) = throw new IllegalArgumentException("label is read only")

    def getLabel = label

    override def load(nbt: NBTTagCompound) {}

    override def save(nbt: NBTTagCompound) {
      if (label != null) {
        nbt.setString(Settings.namespace + "fs.label", label)
      }
    }
  }

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
    with Volatile

  private class BufferedFileSystem(protected val fileRoot: io.File, protected val capacity: Long)
    extends VirtualFileSystem
    with Buffered
    with Capacity {
    // Worst-case: we're on Windows or using a FAT32 partition mounted in *nix.
    // Note: we allow / as the path separator and expect all \s to be converted
    // accordingly before the path is passed to the file system.
    private val invalidChars = """\:*?"<>|""".toSet

    override def makeDirectory(path: String) = super.makeDirectory(validatePath(path))

    override protected def openOutputHandle(id: Int, path: String, mode: Mode) = super.openOutputHandle(id, validatePath(path), mode)

    private def validatePath(path: String) = {
      if (path.exists(invalidChars.contains)) {
        throw new java.io.IOException("path contains invalid characters")
      }
      // TODO Add fix for #338.
      // If on a system with case insensitive file systems, check if path
      // already exists, if so return that name instead (i.e. re-use the
      // existing path, with exact same casing).
      path
    }
  }

}
