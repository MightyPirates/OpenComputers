package li.cil.oc.server.fs

import java.util.zip.ZipFile
import li.cil.oc.api
import li.cil.oc.api.network.Node

object FileSystem {
  def fromClass(clazz: Class[_], domain: String, root: String): Option[api.FileSystem] = {
    val codeSource = clazz.getProtectionDomain.getCodeSource
    if (codeSource == null) return None
    val file = new java.io.File(codeSource.getLocation.toURI)
    if (!file.exists || file.isDirectory) return None
    val zip = new ZipFile(file)
    val path = "/assets/" + domain + "/" + root
    val entry = zip.getEntry(path)
    if (entry == null || !entry.isDirectory) {
      zip.close()
      return None
    }
    Some(new ZipFileSystem(zip, path))
  }

  // TODO
  def asNode(fileSystem: api.FileSystem): Option[Node] = None
}
