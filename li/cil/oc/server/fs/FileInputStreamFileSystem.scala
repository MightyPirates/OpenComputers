package li.cil.oc.server.fs

import java.io
import java.io.FileInputStream

trait FileInputStreamFileSystem {
  protected val root: io.File

  def exists(path: String) = new io.File(root, path).exists()

  def size(path: String) = new io.File(root, path).length()

  def isDirectory(path: String) = new io.File(root, path).isDirectory

  def list(path: String) = Some(new io.File(root, path).listFiles().
    map(file => if (file.isDirectory) file.getName + "/" else file.getName))

  protected def openInputStream(path: String) =
    Some(new FileInputStream(new io.File(root, path)))
}
