package li.cil.oc.server.fs

import java.io
import java.io.FileInputStream

class ReadOnlyFileSystem(val root: io.File) extends InputStreamFileSystem {
  def exists(path: String) = new io.File(root, path).exists()

  def size(path: String) = new io.File(root, path).length()

  def isDirectory(path: String) = new io.File(root, path).isDirectory

  def list(path: String) = Some(new io.File(root, path).list())

  protected def openInputStream(path: String, handle: Long) = Some(new FileInputStream(new io.File(root, path)))
}
