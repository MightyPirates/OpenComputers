package li.cil.oc.server.fs

import java.io
import java.net.URL

import li.cil.oc.api.driver.Container
import li.cil.oc.api.fs.{Label, Mode}
import li.cil.oc.server.component
import li.cil.oc.util.mods.{ComputerCraft15, ComputerCraft16, Mods}
import li.cil.oc.{OpenComputers, Settings, api}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.DimensionManager
import java.util.UUID

object FileSystem extends api.detail.FileSystemAPI {

  lazy val isCaseSensitive = !Settings.get.forceCaseInsensitive && calculateCaseSensitive

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
    var result: Option[api.fs.FileSystem] = None
    if (result.isEmpty && Mods.ComputerCraft16.isAvailable) {
      result = ComputerCraft16.createFileSystem(mount)
    }
    if (result.isEmpty && Mods.ComputerCraft15.isAvailable) {
      result = ComputerCraft15.createFileSystem(mount)
    }
    result.orNull
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

  private def calculateCaseSensitive = {
    val uuid = UUID.randomUUID().toString
    val checkFile1 = new io.File(DimensionManager.getCurrentSaveRootDirectory, uuid + "oc_rox")
    val checkFile2 = new io.File(DimensionManager.getCurrentSaveRootDirectory, uuid + "OC_ROX")
    checkFile2.exists() && checkFile2.delete() // this should NEVER happen but could also lead to VERY weird bugs
    checkFile1.createNewFile()
    val ret = checkFile2.exists()
    checkFile1.delete()
    ret
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

    protected override def segments(path: String) = {
      super.segments(
        if (isCaseSensitive) {
          path
        } else {
          "/" + toCaseInsensitive(withoutSourroundingSlashes(path), root)
        }
      )
    }

    private def validatePath(path: String) = {

      if (path.exists(invalidChars.contains)) {
        throw new java.io.IOException("path contains invalid characters")
      }

      path
    }

    private def withoutSourroundingSlashes(path: String) = {
      val path2 = if (path.startsWith("/"))
        path.substring(1)
      else
        path
      if (path2.endsWith("/"))
        path2.substring(0, path2.length - 1)
      else
        path2
    }

    private def toCaseInsensitive(path: String, node: VirtualDirectory): String = {
      val idx = path.indexOf('/')
      val first = if (idx == -1) path else path.substring(0, idx)
      val rest = if (idx == -1) "" else path.substring(idx + 1)

      val lowerFirst = first.toLowerCase
      var name = first
      node.children.foreach {
        case (childName, child) =>
          if (childName.toLowerCase == lowerFirst) {
            child match {
              case file: VirtualFile =>
                name = childName + "/" + rest
              case dir: VirtualDirectory =>
                name = childName + "/" + toCaseInsensitive(rest, dir)
              case abc: Object =>
                OpenComputers.log.warning(s"[WTF] when resolving case insensitive name, child was a ${abc.getClass.getName}")
            }
          }
      }

      name
    }
  }

}
