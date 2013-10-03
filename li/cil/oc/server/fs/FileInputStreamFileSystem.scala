package li.cil.oc.server.fs

import java.io
import java.io.{FileNotFoundException, FileInputStream}

trait FileInputStreamFileSystem {
  protected val root: io.File

  def exists(path: String) = new io.File(root, path).exists()

  def size(path: String) = new io.File(root, path).length()

  def isDirectory(path: String) = new io.File(root, path).isDirectory

  def list(path: String) = new io.File(root, path) match {
    case file if file.exists() && file.isFile => Some(Array(file.getName))
    case directory if directory.exists() && directory.isDirectory => Some(directory.listFiles().
      map(file => if (file.isDirectory) file.getName + "/" else file.getName))
    case _ => throw new FileNotFoundException("no such file or directory")
  }

  protected def openInputStream(path: String) =
    Some(new FileInputStream(new io.File(root, path)))
}
