package li.cil.oc.server.fs

import java.io.File
import java.util.zip.ZipFile
import li.cil.oc.api
import li.cil.oc.api.network.Node
import li.cil.oc.server.component

object FileSystem extends api.detail.FileSystemAPI {
  def fromClass(clazz: Class[_], domain: String, root: String): Option[api.FileSystem] = {
    val codeSource = clazz.getProtectionDomain.getCodeSource
    if (codeSource == null) return None
    val file = new java.io.File(codeSource.getLocation.toURI)
    if (!file.exists || file.isDirectory) return None
    val path = ("/assets/" + domain + "/" + (root.trim + "/")).replace("//", "/")
    if (file.getName.endsWith(".class")) {
      val fsp = new File(new File(file.getParent), path)
      if (fsp.exists() && fsp.isDirectory)
        Some(new ReadOnlyFileSystem(fsp))
      else
        System.getProperty("java.class.path").split(System.getProperty("path.separator")).
          find(cp => {
          val fsp = new File(new File(cp), path)
          fsp.exists() && fsp.isDirectory
        }) match {
          case None => None
          case Some(dir) => Some(new ReadOnlyFileSystem(new File(new File(dir), path)))
        }
    }
    else {
      val zip = new ZipFile(file)
      val entry = zip.getEntry(path)
      if (entry == null || !entry.isDirectory) {
        zip.close()
        return None
      }
      Some(new ZipFileSystem(zip, path))
    }
  }

  def asNode(fileSystem: api.FileSystem): Option[Node] = Some(new component.FileSystem(fileSystem))
}
