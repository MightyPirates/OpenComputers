package li.cil.oc.server.fs

import cpw.mods.fml.common.Optional
import dan200.computercraft.api.filesystem.{IWritableMount, IMount}
import java.io
import java.net.URL
import li.cil.oc.api.fs.Label
import li.cil.oc.server.component
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

  @Optional.Method(modid = "ComputerCraft")
  def fromComputerCraft(mount: IMount) = new ComputerCraftFileSystem(mount)

  @Optional.Method(modid = "ComputerCraft")
  def fromComputerCraft(mount: IWritableMount) = new ComputerCraftWritableFileSystem(mount)

  def asManagedEnvironment(fileSystem: api.fs.FileSystem, label: Label, container: net.minecraft.tileentity.TileEntity) =
    Option(fileSystem).flatMap(fs => Some(new component.FileSystem(fs, label, Option(container)))).orNull

  def asManagedEnvironment(fileSystem: api.fs.FileSystem, label: String, container: net.minecraft.tileentity.TileEntity) =
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
    with Capacity

}
